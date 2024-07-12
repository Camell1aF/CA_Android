package com.example.ca_test;

import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.MediaController;
import android.widget.TextView;
import android.widget.VideoView;
import androidx.appcompat.app.AppCompatActivity;

public class VideoActivity extends AppCompatActivity {

    private TextView countdownTextView;
    private Handler handler;
    private Runnable runnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video);

        // 设置全屏模式
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        }

        // 确保状态栏和导航栏的颜色透明
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            getWindow().setStatusBarColor(Color.TRANSPARENT);
            getWindow().setNavigationBarColor(Color.TRANSPARENT);
        }

        VideoView videoView = findViewById(R.id.videoView);
        ImageButton closeButton = findViewById(R.id.closeButton);
        countdownTextView = findViewById(R.id.countdownTextView);

        int videoResId = R.raw.aaaa;
        String videoPath = "android.resource://" + getPackageName() + "/" + videoResId;
        Uri videoUri = Uri.parse(videoPath);

        videoView.setVideoURI(videoUri);

        MediaController mediaController = new MediaController(this);
        mediaController.setAnchorView(videoView);
        videoView.setMediaController(mediaController);
        videoView.start();

        closeButton.setOnClickListener(v -> finish());

        startCountdown();
    }

    private void startCountdown() {
        handler = new Handler();
        runnable = new Runnable() {
            int count = 20;

            @Override
            public void run() {
                countdownTextView.setText("正在初始化... " + count + " 秒");
                count--;
                if (count >= 0) {
                    handler.postDelayed(this, 1000);
                } else {
                    countdownTextView.setText("初始化失败，请稍后尝试");
                }
            }
        };
        handler.postDelayed(runnable, 1000);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (handler != null && runnable != null) {
            handler.removeCallbacks(runnable);
        }
    }
}
