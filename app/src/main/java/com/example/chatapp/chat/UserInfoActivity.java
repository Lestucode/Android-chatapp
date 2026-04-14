package com.example.chatapp.chat;

import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.load.model.LazyHeaders;
import com.example.chatapp.R;
import com.example.chatapp.User;
import com.example.chatapp.UserDao;
import com.example.chatapp.chat.AddFriendRequest.HttpClient;
import com.example.chatapp.chat.AddFriendRequest.Result;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.Map;

public class UserInfoActivity extends AppCompatActivity {
    
    private String targetUserId;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_info);
        
        targetUserId = getIntent().getStringExtra("userId");

        MaterialToolbar toolbar = findViewById(R.id.user_info_toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        loadUserInfo();
    }

    private void loadUserInfo() {
        if (targetUserId == null) return;
        
        User user = UserDao.getInstance().getUser();
        if (user == null || user.getToken() == null) return;

        Type type = new TypeToken<Result<Map<String, Object>>>(){}.getType();
        HttpClient.get("/contact/getContactInfo?contactId=" + targetUserId, type, user.getToken(), result -> {
            if (result != null && "success".equals(result.getStatus())) {
                Map<String, Object> data = result.getDataAs(Map.class);
                if (data != null) {
                    runOnUiThread(() -> {
                        TextView tvNickName = findViewById(R.id.tvNickName);
                        TextView tvUserId = findViewById(R.id.tvUserId);
                        TextView tvSignature = findViewById(R.id.tvSignature);
                        ImageView ivAvatar = findViewById(R.id.ivAvatar);

                        tvNickName.setText(data.get("nickName") != null ? String.valueOf(data.get("nickName")) : targetUserId);
                        tvUserId.setText("账号: " + targetUserId);
                        
                        String signature = data.get("personalSignature") != null ? String.valueOf(data.get("personalSignature")) : "这个人很懒，没有留下签名";
                        tvSignature.setText(signature);

                        String avatarUrl = HttpClient.BASE_URL + "/chat/downloadFile?fileId=" + targetUserId + "&showCover=true&t=" + System.currentTimeMillis();
                        GlideUrl glideUrl = new GlideUrl(avatarUrl, new LazyHeaders.Builder()
                                .addHeader("token", user.getToken())
                                .build());
                        Glide.with(this)
                                .load(glideUrl)
                                .circleCrop()
                                .placeholder(R.drawable.dinosaur)
                                .error(R.drawable.dinosaur)
                                .into(ivAvatar);
                    });
                }
            } else {
                runOnUiThread(() -> Toast.makeText(UserInfoActivity.this, "加载用户信息失败", Toast.LENGTH_SHORT).show());
            }
        });
    }
}