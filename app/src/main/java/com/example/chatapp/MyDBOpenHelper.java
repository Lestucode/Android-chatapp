package com.example.chatapp;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class MyDBOpenHelper extends SQLiteOpenHelper {
    private static final String DBNAME = "User.db";
    private static final int VERSION = 3;  // 版本+1，让数据库自动升级

    public MyDBOpenHelper(Context context) {
        super(context, DBNAME, null, VERSION);
    }

    // 创建数据库（正确字段）
    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE user_info ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "userId TEXT,"
                + "nickname TEXT,"
                + "email TEXT,"      // 加了这里
                + "token TEXT"
                + ")");
    }

    // 升级数据库
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS user_info");
        onCreate(db);
    }
}