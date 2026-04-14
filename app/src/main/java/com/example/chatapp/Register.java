package com.example.chatapp;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;
import android.content.Intent;

import androidx.appcompat.app.AppCompatActivity;

import com.example.chatapp.chat.AddFriendRequest.HttpClient;
import com.example.chatapp.chat.AddFriendRequest.Result;
import com.google.android.material.textfield.TextInputEditText;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class Register extends AppCompatActivity {
    // UI 组件
    private TextInputEditText register_email, register_nickName, register_password1, register_password2, register_checkCode;
    private Button register_button;
    private ImageView register_checkCodeImage;
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    // 用户输入
    private String nickName;
    private String password;
    private String email;
    private String checkCodeKey;
    //logcat报错
    private static final String TAG = "RegisterActivity";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        // 初始化 UI 组件
        register_email = findViewById(R.id.register_email);
        register_nickName = findViewById(R.id.register_nickname);
        register_password1 = findViewById(R.id.register_password1);
        register_password2 = findViewById(R.id.register_password2);
        register_checkCode = findViewById(R.id.register_check_code);
        register_checkCodeImage = findViewById(R.id.register_check_code_image);
        register_button = findViewById(R.id.register_button);

        // 加载验证码
        loadCheckCode();

        // 点击验证码图片刷新验证码
        register_checkCodeImage.setOnClickListener(v -> loadCheckCode());

        // 点击注册按钮时触发
        register_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // 获取输入的昵称和邮箱
                String nickName = register_nickName.getText().toString().trim();
                String email = register_email.getText().toString().trim();
                String checkCode = register_checkCode.getText().toString().trim();

                // 验证昵称和邮箱是否为空
                if (nickName.isEmpty() || email.isEmpty()) {
                    Toast.makeText(Register.this, "昵称与邮箱不得为空", Toast.LENGTH_SHORT).show();
                    return;
                }

                // 验证验证码是否为空
                if (checkCode.isEmpty()) {
                    Toast.makeText(Register.this, "请输入验证码", Toast.LENGTH_SHORT).show();
                    return;
                }

                // 验证密码输入是否有效
                if (!checkPwd()) {
                    // 密码验证失败，提示已在 checkPwd() 中显示
                    return;
                }

                // 密码验证通过后，获取密码值（两次输入已一致）
                String password = register_password1.getText().toString().trim();

                // 构建注册参数
                Map<String, Object> params = new HashMap<>();
                params.put("checkCodeKey", checkCodeKey);
                params.put("email", email);
                params.put("password", password);
                params.put("nickName", nickName);
                params.put("checkCode", checkCode);

                // 调用注册接口
                Type registerType = new TypeToken<Result<Map<String, Object>>>(){}.getType();
                HttpClient.post("/account/register", params, null, registerType, registerResult -> {
                    if (registerResult != null && "success".equals(registerResult.getStatus())) {
                        runOnUiThread(() -> {
                            Toast.makeText(Register.this, "注册成功", Toast.LENGTH_LONG).show();
                            // 跳转到登录界面
                            Intent intent = new Intent(Register.this, MainActivity.class);
                            startActivity(intent);
                            Register.this.finish();
                        });
                    } else {
                        runOnUiThread(() -> {
                            Toast.makeText(Register.this, "注册失败: " + (registerResult != null ? registerResult.getInfo() : "未知错误"), Toast.LENGTH_SHORT).show();
                            // 刷新验证码
                            loadCheckCode();
                        });
                    }
                });
            }
        });
    }

    /**
     * 加载验证码
     */
    private void loadCheckCode() {
        Type checkCodeType = new TypeToken<Result<Map<String, String>>>(){}.getType();
        HttpClient.get("/account/checkCode", checkCodeType, null, result -> {
            if (result != null && "success".equals(result.getStatus())) {
                // 使用类型转换方法获取数据
                Map<String, String> data = result.getDataAs(Map.class);
                if (data != null) {
                    checkCodeKey = data.get("checkCodeKey");
                    String checkCodeBase64 = data.get("checkCode");

                    // 显示验证码图片
                    if (checkCodeBase64 != null && !checkCodeBase64.isEmpty()) {
                        try {

                            if (checkCodeBase64.startsWith("data:image")) {
                                checkCodeBase64 = checkCodeBase64.split(",")[1];
                            }

                            byte[] decodedBytes = Base64.decode(checkCodeBase64, Base64.DEFAULT);
                            Bitmap bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
                            new Handler(Looper.getMainLooper()).post(() -> {
                                register_checkCodeImage.setImageBitmap(bitmap);
                            });
                        } catch (Exception e) {
                            Log.e(TAG, "验证码图片解码失败", e);
                        }
                    }
                }
            } else {
                runOnUiThread(() ->
                        Toast.makeText(Register.this, "获取验证码失败", Toast.LENGTH_SHORT).show()
                );
            }
        });
    }

    /**
     * 检查两次密码输入是否一致且符合要求
     * @return true 如果密码有效，false 如果无效并显示提示信息
     */
    private boolean checkPwd() {
        String pwd1 = register_password1.getText().toString().trim();
        String pwd2 = register_password2.getText().toString().trim();
        if (pwd1.isEmpty() || pwd2.isEmpty()) {
            Toast.makeText(Register.this, "请输入密码", Toast.LENGTH_SHORT).show();
            return false;
        } else if (!pwd1.equals(pwd2)) {
            Toast.makeText(Register.this, "两次输入的密码不一致", Toast.LENGTH_SHORT).show();
            return false;
        } else if (pwd1.length() < 6) {
            Toast.makeText(Register.this, "密码需超过6位数", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }
}
