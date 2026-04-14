package com.example.chatapp;

import static android.content.ContentValues.TAG;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Base64;
import android.util.Log;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.example.chatapp.chat.AddFriendRequest.HttpClient;
import com.example.chatapp.chat.AddFriendRequest.Result;
import com.example.chatapp.chat.ChatHomePage;
import com.google.android.material.textfield.TextInputEditText;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {
    private TextView loginRegister;
    private TextInputEditText loginPassword, loginEmail, loginCheckCode;
    private ImageView loginCheckCodeImage;
    private Button loginBtn;
    private String checkCodeKey;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);

        loginRegister = findViewById(R.id.login_register);
        loginPassword = findViewById(R.id.login_password);
        loginBtn = findViewById(R.id.login_button);
        loginEmail = findViewById(R.id.login_email); // 注意：这里是账号输入框，对应后端的email
        loginCheckCode = findViewById(R.id.login_check_code);
        loginCheckCodeImage = findViewById(R.id.login_check_code_image);
        UserDao userDao = UserDao.getInstance();

        // 加载验证码
        loadCheckCode();

        // 点击验证码图片刷新验证码
        loginCheckCodeImage.setOnClickListener(v -> loadCheckCode());

        // 注册跳转
        loginRegister.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, Register.class);
            startActivity(intent);
        });

        // 登录按钮
        loginBtn.setOnClickListener(v -> loginCheck());
    }

    /**
     * 加载验证码
     */
    private void loadCheckCode() {
        Log.d(TAG, "loadCheckCode: start");
        Type checkCodeType = new TypeToken<Result<Map<String, String>>>(){}.getType();
        HttpClient.get("/account/checkCode", checkCodeType, null, result -> {
            if (result == null) {
                Log.e(TAG, "loadCheckCode: result is null");
                runOnUiThread(() ->
                        Toast.makeText(MainActivity.this, "获取验证码失败: 请求失败或无响应", Toast.LENGTH_SHORT).show()
                );
                return;
            }

            Log.d(TAG, "loadCheckCode: status=" + result.getStatus() + ", code=" + result.getCode() + ", info=" + result.getInfo());
            if (result != null && "success".equals(result.getStatus())) {
                // 使用类型转换方法获取数据
                Map<String, String> data = result.getDataAs(Map.class);

                if (data != null) {
                    Log.d(TAG, "loadCheckCode: data keys=" + data.keySet());
                    checkCodeKey = data.get("checkCodeKey");
                    String checkCodeBase64 = data.get("checkCode");
                    Log.d(TAG, "loadCheckCode: checkCodeKey=" + checkCodeKey);
                    Log.d(TAG, "loadCheckCode: checkCodeBase64 length=" + (checkCodeBase64 != null ? checkCodeBase64.length() : -1));

                    // 显示验证码图片
                    if (checkCodeBase64 != null && !checkCodeBase64.isEmpty()) {
                        // Base64解码并显示图片
                        try {
                            // 1. 清理Base64前缀（如果有的话）
                            String cleanBase64 = checkCodeBase64;
                            if (checkCodeBase64.contains(",")) {
                                cleanBase64 = checkCodeBase64.split(",")[1];
                                Log.d(TAG, "已清理Base64前缀");
                            }

                            int previewLen = Math.min(40, cleanBase64.length());
                            Log.d(TAG, "loadCheckCode: cleanBase64 preview=" + cleanBase64.substring(0, previewLen));
                            
                            byte[] decodedBytes = Base64.decode(cleanBase64, Base64.DEFAULT);
                            Log.d(TAG, "Base64解码成功，长度: " + decodedBytes.length);
                            
                            // 3. 解码成功后显示
                            if (decodedBytes.length > 0) {
                                Bitmap bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
                                if (bitmap != null) {
                                    Log.d(TAG, "Bitmap解码成功，宽: " + bitmap.getWidth() + ", 高: " + bitmap.getHeight());
                                    new Handler(Looper.getMainLooper()).post(() -> {
                                        Log.d(TAG, "loadCheckCode: setImageBitmap on main thread, imageView=" + loginCheckCodeImage);
                                        loginCheckCodeImage.setImageBitmap(bitmap);
                                    });
                                } else {
                                    Log.e(TAG, "Bitmap解码失败: 无法创建Bitmap");
                                    runOnUiThread(() -> {
                                        Toast.makeText(MainActivity.this, "验证码图片解码失败", Toast.LENGTH_SHORT).show();
                                    });
                                }
                            } else {
                                Log.e(TAG, "Base64解码后为空");
                                runOnUiThread(() -> {
                                    Toast.makeText(MainActivity.this, "验证码数据为空", Toast.LENGTH_SHORT).show();
                                });
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "验证码图片解码失败", e);
                            runOnUiThread(() -> {
                                Toast.makeText(MainActivity.this, "验证码处理失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            });
                        }
                    } else {
                        Log.e(TAG, "checkCodeBase64为空");
                        runOnUiThread(() -> {
                            Toast.makeText(MainActivity.this, "验证码数据为空", Toast.LENGTH_SHORT).show();
                        });
                    }
                } else {
                    Log.e(TAG, "data为空");
                    runOnUiThread(() -> {
                        Toast.makeText(MainActivity.this, "验证码数据异常", Toast.LENGTH_SHORT).show();
                    });
                }
            } else {
                Log.e(TAG, "loadCheckCode: not success, status=" + result.getStatus() + ", code=" + result.getCode() + ", info=" + result.getInfo());
                runOnUiThread(() ->
                        Toast.makeText(MainActivity.this, "获取验证码失败: " + (result != null ? result.getInfo() : "未知错误"), Toast.LENGTH_SHORT).show()
                );
            }
        });
    }

    private void loginCheck() {
        String email = loginEmail.getText() != null ? loginEmail.getText().toString().trim() : "";
        String password = loginPassword.getText() != null ? loginPassword.getText().toString().trim() : "";
        String finalpassword = MD5Util.encode(password);
        String checkCode = loginCheckCode.getText() != null ? loginCheckCode.getText().toString().trim() : "";

        Log.d(TAG, "loginCheck: email=" + email + ", checkCodeKey=" + checkCodeKey + ", checkCodeLen=" + (checkCode != null ? checkCode.length() : -1));

        if (email.isEmpty() || password.isEmpty()) {
            runOnUiThread(() ->
                    Toast.makeText(MainActivity.this, "邮箱或密码不能为空", Toast.LENGTH_SHORT).show()
            );
            return;
        }

        if (checkCode.isEmpty()) {
            runOnUiThread(() ->
                    Toast.makeText(MainActivity.this, "请输入验证码", Toast.LENGTH_SHORT).show()
            );
            return;
        }

        // 构建登录参数
        Map<String, Object> params = new HashMap<>();
        params.put("checkCodeKey", checkCodeKey);
        params.put("email", email);
        params.put("password", finalpassword);
        params.put("checkCode", checkCode);

        // 调用登录接口
        Type loginType = new TypeToken<Result<Map<String, Object>>>(){}.getType();
        HttpClient.post("/account/login", params, null, loginType, loginResult -> {
            if (loginResult == null) {
                Log.e(TAG, "loginCheck: loginResult is null");
            } else {
                Log.d(TAG, "loginCheck: status=" + loginResult.getStatus() + ", code=" + loginResult.getCode() + ", info=" + loginResult.getInfo());
            }
            if (loginResult != null && "success".equals(loginResult.getStatus())) {
                // 使用类型转换方法获取数据
                Map<String, Object> userData = loginResult.getDataAs(Map.class);
                Log.e("后端返回的用户数据", new Gson().toJson(userData));

                if (userData != null) {
                    // 手动创建UserInfo对象
                    UserInfo userInfo = new UserInfo();
                    userInfo.setUserId(userData.get("userId").toString());
                    userInfo.setEmail(email.toString());
                    userInfo.setNickName(userData.get("nickName").toString());
                    userInfo.setToken(userData.get("token").toString());
                    Log.e("登录成功", "======================");
                    Log.e("登录成功", "返回的 userId：" + userInfo.getUserId());
                    Log.e("登录成功", "返回的 token：" + userInfo.getToken());
                    Log.e("登录成功", "返回的 nickName：" + userInfo.getNickName());
                    
                    // 存到 SQLite
                    UserDao userDao = UserDao.getInstance();
                    Log.e("登录成功", "已执行 saveUser 保存到 SQLite");
                    userDao.saveUser(userInfo.getEmail(), userInfo.getNickName(), userInfo.getUserId(), userInfo.getToken());

                    runOnUiThread(() -> {
                        Toast.makeText(MainActivity.this, "登录成功", Toast.LENGTH_SHORT).show();

                        // 登录成功后启动 WebSocketService
                        MyApplication application = (MyApplication) getApplication();
                        application.startWebSocketService();

                        Intent intent = new Intent(MainActivity.this, ChatHomePage.class);
                        startActivity(intent);
                        finish();
                    });
                }
            } else {
                runOnUiThread(() -> {
                    Toast.makeText(MainActivity.this, "登录失败: " + (loginResult != null ? loginResult.getInfo() : "未知错误"), Toast.LENGTH_SHORT).show();
                    // 刷新验证码
                    loadCheckCode();
                });
            }
        });
    }

    // 用户信息类
    public static class UserInfo {
        private String userId;
        private String email;
        private String nickName;
        private String token;

        public String getUserId() {
            return userId;
        }

        public void setUserId(String userId) {
            this.userId = userId;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getNickName() {
            return nickName;
        }

        public void setNickName(String nickName) {
            this.nickName = nickName;
        }

        public String getToken() {
            return token;
        }

        public void setToken(String token) {
            this.token = token;
        }
    }
}
