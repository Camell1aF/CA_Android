package com.example.ca_test;
// 定义包名为 com.example.ca_test

import android.net.Uri;
// 导入 Uri 类，用于表示视频的路径

import android.os.Bundle;
// 导入 Bundle 类，用于在活动之间传递数据

import android.os.Handler;
// 导入 Handler 类，用于处理消息和可运行对象

import android.view.View;
// 导入 View 类，用于处理 UI 组件

import android.widget.ImageButton;
// 导入 ImageButton 类，用于处理图像按钮

import android.widget.MediaController;
// 导入 MediaController 类，用于控制视频播放

import android.widget.TextView;
// 导入 TextView 类，用于显示文本

import android.widget.VideoView;
// 导入 VideoView 类，用于播放视频

import androidx.appcompat.app.AppCompatActivity;
// 导入 AppCompatActivity 类，所有 Activity 都应继承这个类以支持较老版本的 Android

public class VideoActivity extends AppCompatActivity {
// 定义 VideoActivity 类并继承 AppCompatActivity 类

    private TextView countdownTextView;
    // 声明一个 TextView 对象，用于显示倒计时文本

    private Handler handler;
    // 声明一个 Handler 对象，用于处理消息和可运行对象

    private Runnable runnable;
    // 声明一个 Runnable 对象，用于在特定时间内执行的任务

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // 重写 onCreate 方法，活动创建时调用

        super.onCreate(savedInstanceState);
        // 调用父类的 onCreate 方法

        setContentView(R.layout.activity_video);
        // 设置当前活动的布局文件为 activity_video

        VideoView videoView = findViewById(R.id.videoView);
        // 通过 ID 获取布局文件中的 VideoView 控件

        ImageButton closeButton = findViewById(R.id.closeButton);
        // 通过 ID 获取布局文件中的关闭按钮

        countdownTextView = findViewById(R.id.countdownTextView);
        // 通过 ID 获取布局文件中的倒计时文本

        int videoResId = R.raw.aaaa;
        // 获取视频资源的资源 ID

        String videoPath = "android.resource://" + getPackageName() + "/" + videoResId;
        // 设置视频路径

        Uri videoUri = Uri.parse(videoPath);
        // 设置视频 Uri

        videoView.setVideoURI(videoUri);
        // 设置 VideoView 的视频路径

        MediaController mediaController = new MediaController(this);
        // 创建 MediaController 对象，用于控制视频播放

        mediaController.setAnchorView(videoView);
        // 将 MediaController 锚定到 VideoView

        videoView.setMediaController(mediaController);
        // 将 MediaController 与 VideoView 绑定

        videoView.start();
        // 准备并开始播放视频

        closeButton.setOnClickListener(v -> finish());
        // 设置关闭按钮的点击事件，点击时结束活动

        startCountdown();
        // 开始倒计时
    }

    private void startCountdown() {
        // 开始倒计时的方法

        handler = new Handler();
        // 初始化 Handler 对象

        runnable = new Runnable() {
            int count = 20;
            // 倒计时开始时间为 20 秒

            @Override
            public void run() {
                countdownTextView.setText("正在初始化... " + count + " 秒");
                // 更新倒计时文本

                count--;
                // 每秒减一

                if (count >= 0) {
                    handler.postDelayed(this, 1000);
                    // 每隔一秒执行一次
                } else {
                    countdownTextView.setText("初始化失败，请稍后尝试");
                    // 倒计时结束，显示初始化失败提示
                }
            }
        };

        handler.postDelayed(runnable, 1000);
        // 延迟一秒开始倒计时
    }

    @Override
    protected void onDestroy() {
        // 重写 onDestroy 方法，活动销毁时调用

        super.onDestroy();
        // 调用父类的 onDestroy 方法

        if (handler != null && runnable != null) {
            handler.removeCallbacks(runnable);
            // 如果 handler 和 runnable 不为空，移除未完成的 runnable
        }
    }
}
