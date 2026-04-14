package com.example.chatapp;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.example.chatapp.chat.UserSearchAdapter;

public class UserDao {
    private MyDBOpenHelper dbHelper;
    private static UserDao instance;

    private UserDao(Context context) {
        dbHelper = new MyDBOpenHelper(context);
        
    }
    public static UserDao initialize(Context context) {
        if (instance == null) {
            instance = new UserDao(context);
        }
        return instance;
    }

    // 供 Service 和 Fragment 调用，获取单例
    public static UserDao getInstance() {
        if (instance == null) {
            throw new IllegalStateException("UserDao 未初始化！请确保 MyApplication 已正确注册并调用了 initialize(Context)。");
        }
        return instance;    }


    // 保存或更新用户
    public void saveUser(String email, String nickname,String userId,String token) {
        Log.e("数据库保存", "======================");
        Log.e("数据库保存", "准备保存到 SQLite");
        Log.e("数据库保存", "email => " + email);
        Log.e("数据库保存", "nickname => " + nickname);
        Log.e("数据库保存", "userId => " + userId);
        Log.e("数据库保存", "token => " + token);
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.delete("user_info", null, null); // 先清空（只保存一个用户）

        ContentValues values = new ContentValues();
        values.put("email", email);
        values.put("nickname", nickname);
        values.put("token", token);
        values.put("userId", userId);
        db.insert("user_info", null, values);

        db.close();
    }

    // 获取用户
    public User getUser() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query("user_info", null, null, null, null, null, null);
        User user = null;
        if (cursor.moveToFirst()) {
            user = new User();
            user.setEmail(cursor.getString(cursor.getColumnIndexOrThrow("email")));
            user.setNickname(cursor.getString(cursor.getColumnIndexOrThrow("nickname")));
            user.setUserId(cursor.getString(cursor.getColumnIndexOrThrow("userId")));
            user.setToken(cursor.getString(cursor.getColumnIndexOrThrow("token")));
        }
        cursor.close();
        db.close();
        return user;
    }

    // 清空用户信息（退出登录/Token失效时用）
    public void clearUser() {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.delete("user_info", null, null);
        db.close();
    }
}
