package com.example.chatapp;

import android.app.Application;
import android.content.Intent;
import android.os.Build;

import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

public class MyApplication extends Application {

    private static MyApplication instance;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        // 初始化其他组件（如 UserDao）
        UserDao.initialize(getApplicationContext());
    }

    public static MyApplication getInstance() {
        return instance;
    }

    public void handleTokenExpired() {
        new Handler(Looper.getMainLooper()).post(() -> {
            Toast.makeText(this, "登录已超时，请重新登录", Toast.LENGTH_LONG).show();
            UserDao.getInstance().clearUser();
            
            // Stop WebSocketService
            stopService(new Intent(this, WebSocketService.class));
            
            // Jump to Login (MainActivity)
            Intent intent = new Intent(this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });
    }

    public void handleForceOffline() {
        new Handler(Looper.getMainLooper()).post(() -> {
            Toast.makeText(this, "账号已在其他设备登录，请重新登录", Toast.LENGTH_LONG).show();
            UserDao.getInstance().clearUser();
            stopService(new Intent(this, WebSocketService.class));
            Intent intent = new Intent(this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });
    }

    public void startWebSocketService() {
        UserDao userDao = UserDao.getInstance();
        User user = userDao.getUser();
        if (user == null || user.getToken() == null || user.getUserId() == null) {
            return;
        }

        Intent intent = new Intent(this, WebSocketService.class);
        intent.putExtra(WebSocketService.EXTRA_USER_ID, user.getUserId());
        intent.putExtra(WebSocketService.EXTRA_TOKEN, user.getToken());

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent);
        } else {
            startService(intent);
        }
    }
}
