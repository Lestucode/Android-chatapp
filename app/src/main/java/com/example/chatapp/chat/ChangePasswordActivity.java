package com.example.chatapp.chat;

import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.chatapp.R;
import com.example.chatapp.User;
import com.example.chatapp.UserDao;
import com.example.chatapp.chat.AddFriendRequest.HttpClient;
import com.example.chatapp.chat.AddFriendRequest.Result;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

public class ChangePasswordActivity extends AppCompatActivity {

    private MaterialToolbar toolbar;
    private TextInputEditText etNewPassword;
    private TextInputEditText etConfirmPassword;
    private MaterialButton btnChangePassword;

    private static final String PASSWORD_REGEX = "^(?=.*\\d)(?=.*[a-zA-Z])[\\da-zA-Z~!@#$%^&*_]{8,18}$";
    private static final Pattern PASSWORD_PATTERN = Pattern.compile(PASSWORD_REGEX);

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_change_password);

        toolbar = findViewById(R.id.change_password_toolbar);
        etNewPassword = findViewById(R.id.et_new_password);
        etConfirmPassword = findViewById(R.id.et_confirm_password);
        btnChangePassword = findViewById(R.id.btn_change_password);

        toolbar.setNavigationOnClickListener(v -> finish());

        btnChangePassword.setOnClickListener(v -> changePassword());
    }

    private void changePassword() {
        String newPassword = etNewPassword.getText().toString().trim();
        String confirmPassword = etConfirmPassword.getText().toString().trim();

        if (newPassword.isEmpty()) {
            Toast.makeText(this, "请输入新密码", Toast.LENGTH_SHORT).show();
            return;
        }

        if (confirmPassword.isEmpty()) {
            Toast.makeText(this, "请确认新密码", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!newPassword.equals(confirmPassword)) {
            Toast.makeText(this, "两次输入的密码不一致", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!PASSWORD_PATTERN.matcher(newPassword).matches()) {
            Toast.makeText(this, "密码格式不正确，需要8-18位且包含字母和数字", Toast.LENGTH_SHORT).show();
            return;
        }

        User user = UserDao.getInstance().getUser();
        if (user == null || user.getToken() == null) {
            Toast.makeText(this, "用户未登录", Toast.LENGTH_SHORT).show();
            return;
        }

        btnChangePassword.setEnabled(false);
        btnChangePassword.setText("修改中...");

        Map<String, Object> params = new HashMap<>();
        params.put("password", newPassword);

        Type type = new TypeToken<Result<Object>>(){}.getType();
        HttpClient.post("/userInfo/updatePassword", params, user.getToken(), type, result -> {
            runOnUiThread(() -> {
                btnChangePassword.setEnabled(true);
                btnChangePassword.setText("确认修改");

                if (result != null && result.getCode() != null && result.getCode() == 200) {
                    Toast.makeText(this, "密码修改成功，请重新登录", Toast.LENGTH_LONG).show();
                    // 密码修改成功后，后端会关闭WebSocket连接，跳转到登录页
                    UserDao.getInstance().clearUser();
                    android.content.Intent intent = new android.content.Intent(this, com.example.chatapp.MainActivity.class);
                    intent.setFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK | android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                } else {
                    String errorMsg = result != null ? result.getInfo() : "请求失败";
                    Toast.makeText(this, "修改失败: " + errorMsg, Toast.LENGTH_SHORT).show();
                }
            });
        });
    }
}
