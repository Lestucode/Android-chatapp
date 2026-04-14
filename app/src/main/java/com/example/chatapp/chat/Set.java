package com.example.chatapp.chat;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.example.chatapp.MainActivity;
import com.example.chatapp.R;
import com.example.chatapp.User;
import com.example.chatapp.UserDao;
import com.example.chatapp.WebSocketService;
import com.example.chatapp.database.AppDatabase;
import com.example.chatapp.util.AppExecutors;
import com.example.chatapp.chat.AddFriendRequest.HttpClient;
import com.example.chatapp.chat.AddFriendRequest.Result;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.load.model.LazyHeaders;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import android.graphics.drawable.Drawable;

public class Set extends AppCompatActivity {
    private static final String TAG = "SetActivity";

    private MaterialToolbar toolbar;
    private ImageView ivAvatar;
    private Button btnChangeAvatar;
    private EditText etNickname;
    private EditText etSignature;
    private Button btnSave;
    private Button btnLogout;

    private File selectedAvatarFile = null;

    private final ActivityResultLauncher<String> pickImageLauncher = 
        registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
            if (uri != null) {
                // 显示选中的图片
                Glide.with(this)
                        .load(uri)
                        .circleCrop()
                        .into(ivAvatar);
                // 将 Uri 转换为临时文件，以便上传
                selectedAvatarFile = getFileFromUri(uri);
            }
        });

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.set);

        toolbar = findViewById(R.id.set_toolbar);
        ivAvatar = findViewById(R.id.iv_avatar);
        btnChangeAvatar = findViewById(R.id.btn_change_avatar);
        etNickname = findViewById(R.id.et_nickname);
        etSignature = findViewById(R.id.et_signature);
        btnSave = findViewById(R.id.btn_save);
        btnLogout = findViewById(R.id.btn_logout);

        // 设置返回按钮点击事件
        toolbar.setNavigationOnClickListener(v -> finish());

        fetchUserInfo();

        btnChangeAvatar.setOnClickListener(v -> pickImageLauncher.launch("image/*"));
        btnSave.setOnClickListener(v -> saveUserInfo());
        btnLogout.setOnClickListener(v -> performLogout());
    }

    private void fetchUserInfo() {
        User user = UserDao.getInstance().getUser();
        if (user == null || user.getToken() == null) {
            Toast.makeText(this, "用户未登录", Toast.LENGTH_SHORT).show();
            return;
        }

        Type respType = new TypeToken<Result<Map<String, Object>>>(){}.getType();
        HttpClient.get("/userInfo/getUserInfo", respType, user.getToken(), result -> {
            if (result != null && result.getCode() != null && result.getCode() == 200) {
                Map<String, Object> info = result.getDataAs(Map.class);
                if (info != null) {
                    runOnUiThread(() -> {
                        Object nickName = info.get("nickName");
                        Object signature = info.get("personalSignature");
                        
                        if (nickName != null) etNickname.setText(nickName.toString());
                        if (signature != null) etSignature.setText(signature.toString());

                        // 加载头像
                        String avatarUrl = "http://82.157.200.53:5050/api/chat/downloadFile?fileId=" + user.getUserId() + "&showCover=true";
                        
                        // 后端的 downloadFile 接口加了 @GlobalInterceptor，必须携带 token 请求头
                        GlideUrl glideUrl = new GlideUrl(avatarUrl, new LazyHeaders.Builder()
                                .addHeader("token", user.getToken())
                                .build());

                        Glide.with(this)
                            .load(glideUrl)
                            .placeholder(R.drawable.dinosaur)
                            .error(R.drawable.dinosaur)
                            .circleCrop()
                            .listener(new RequestListener<Drawable>() {
                                @Override
                                public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                                    Log.e(TAG, "Glide 加载头像失败", e);
                                    return false;
                                }

                                @Override
                                public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                                    Log.d(TAG, "Glide 加载头像成功");
                                    return false;
                                }
                            })
                            // 禁用缓存，或者添加 signature 以便更新时能重新加载
                            .diskCacheStrategy(DiskCacheStrategy.NONE)
                            .skipMemoryCache(true)
                            .into(ivAvatar);
                    });
                }
            } else {
                runOnUiThread(() -> Toast.makeText(this, "获取个人信息失败", Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void saveUserInfo() {
        User user = UserDao.getInstance().getUser();
        if (user == null || user.getToken() == null) return;

        String nickname = etNickname.getText().toString().trim();
        String signature = etSignature.getText().toString().trim();

        if (nickname.isEmpty()) {
            Toast.makeText(this, "昵称不能为空", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> params = new HashMap<>();
        params.put("nickName", nickname);
        params.put("personalSignature", signature);

        Type type = new TypeToken<Result<Map<String, Object>>>(){}.getType();
        
        Map<String, File> files = new HashMap<>();
        files.put("avatarFile", selectedAvatarFile);
        files.put("avatarCover", selectedAvatarFile);
        HttpClient.uploadFiles("/userInfo/saveUserInfo", params, files, user.getToken(), type, result -> {
            runOnUiThread(() -> {
                if (result != null && result.getCode() != null && result.getCode() == 200) {
                    Toast.makeText(this, "保存成功", Toast.LENGTH_SHORT).show();
                    // 更新成功后，清空已选文件，防止重复上传
                    selectedAvatarFile = null;
                    
                    // 为了让其他页面也能刷新头像，可以在这里发送一个本地广播
                    Intent intent = new Intent("com.example.chatapp.USER_INFO_UPDATED");
                    androidx.localbroadcastmanager.content.LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
                    
                    // 更新本地用户数据的昵称
                    user.setNickname(nickname);
                    UserDao.getInstance().saveUser(user.getEmail(), nickname, user.getUserId(), user.getToken());
                } else {
                    Toast.makeText(this, "保存失败: " + (result != null ? result.getInfo() : "未知错误"), Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    private File getFileFromUri(Uri uri) {
        try {
            InputStream is = getContentResolver().openInputStream(uri);
            File tempFile = File.createTempFile("avatar", ".jpg", getCacheDir());
            FileOutputStream fos = new FileOutputStream(tempFile);
            byte[] buffer = new byte[1024];
            int len;
            while ((len = is.read(buffer)) > 0) {
                fos.write(buffer, 0, len);
            }
            fos.close();
            is.close();
            return tempFile;
        } catch (Exception e) {
            Log.e(TAG, "解析图片 URI 失败", e);
            return null;
        }
    }

    private void performLogout() {
        User user = UserDao.getInstance().getUser();
        if (user == null || user.getToken() == null) return;

        Type type = new TypeToken<Result<Object>>(){}.getType();
        HttpClient.post("/userInfo/logout", new HashMap<>(), user.getToken(), type, result -> {
            // 无论服务端是否返回 success，客户端都应该执行退出清理逻辑
            runOnUiThread(() -> {
                // 先发指令手动停止 WebSocketService
                Intent stopIntent = new Intent(Set.this, WebSocketService.class);
                stopIntent.setAction(WebSocketService.ACTION_MANUAL_STOP);
                startService(stopIntent);

                // 再清理本地数据
                UserDao.getInstance().clearUser();
                
                AppExecutors.io().execute(() -> {
                    try {
                        AppDatabase db = AppDatabase.getInstance(getApplicationContext());
                        db.clearAllTables(); // 清空 Room 所有聊天数据
                    } catch (Exception e) {
                        Log.e(TAG, "清理 Room 失败", e);
                    } finally {
                        AppDatabase.destroyInstance(); // 销毁实例，防止复用旧引用
                    }

                    runOnUiThread(() -> {
                        Toast.makeText(Set.this, "已退出登录", Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(Set.this, MainActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();
                    });
                });
            });
        });
    }
}
