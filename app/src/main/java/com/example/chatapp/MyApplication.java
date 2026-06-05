package com.example.chatapp;

import android.app.Application;
import android.content.Intent;
import android.os.Build;

import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import com.example.chatapp.database.AppDatabase;
import com.example.chatapp.util.AppExecutors;

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

    /**
     * Token 过期处理 - 必须清理 Room 数据库，防止换账号串消息
     */
    public void handleTokenExpired() {
        new Handler(Looper.getMainLooper()).post(() -> {
            Toast.makeText(this, "登录已超时，请重新登录", Toast.LENGTH_LONG).show();
            performClearAndLogout();
        });
    }

    /**
     * 强制下线处理 - 必须清理 Room 数据库，防止换账号串消息
     */
    public void handleForceOffline() {
        new Handler(Looper.getMainLooper()).post(() -> {
            Toast.makeText(this, "账号已在其他设备登录，请重新登录", Toast.LENGTH_LONG).show();
            performClearAndLogout();
        });
    }

    /**
     * 统一清理逻辑：清空 UserDao + Room 数据库 + 销毁 Room 实例 + 停止 WebSocket
     */
    private void performClearAndLogout() {
        // 1. 清理用户 Token 信息
        UserDao.getInstance().clearUser();

        // 2. 异步清理 Room 数据库（防止主线程阻塞），清理后销毁实例
        AppExecutors.io().execute(() -> {
            try {
                AppDatabase db = AppDatabase.getInstance(this);
                db.clearAllTables();
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                AppDatabase.destroyInstance();
            }
        });

        // 3. 停止 WebSocketService
        stopService(new Intent(this, WebSocketService.class));

        // 4. 跳转到登录页
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
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
