package com.example.chatapp;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import androidx.appcompat.app.AppCompatActivity;
import com.example.chatapp.chat.ChatHomePage;

public class SplashActivity extends AppCompatActivity {
    private static final String TAG = "自动登录检查";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.e(TAG, "==================== 启动页开始运行 ====================");

        UserDao userDao = UserDao.getInstance();
        User user = userDao.getUser();

        // ============== 日志 1：有没有读到用户 ==============
        if (user == null) {
            Log.e(TAG, "结果：本地没有任何用户 → 必须重新登录");
            goToLogin();
            return;
        }

        Log.e(TAG, "结果：读到本地用户 → " + user.getUserId());
        Log.e(TAG, "本地Token → " + user.getToken());

        // ============== 日志 2：Token 是否为空 ==============
        if (user.getToken() == null || user.getToken().isEmpty()) {
            Log.e(TAG, "结果：Token为空 → 重新登录");
            goToLogin();
            return;
        }

        Log.e(TAG, "结果：Token有效，直接进入主页！");

        // 启动服务
        MyApplication application = (MyApplication) getApplication();
        application.startWebSocketService();

        goToHome();
    }

    private void goToHome() {
        startActivity(new Intent(this, ChatHomePage.class));
        finish();
    }

    private void goToLogin() {
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }
}