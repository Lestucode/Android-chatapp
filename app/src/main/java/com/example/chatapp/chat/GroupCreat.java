package com.example.chatapp.chat;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.chatapp.R;
import com.example.chatapp.User;
import com.example.chatapp.UserDao;
import com.example.chatapp.chat.AddFriendRequest.HttpClient;
import com.example.chatapp.chat.AddFriendRequest.Result;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

public class GroupCreat extends AppCompatActivity {

    private ImageView ivGroupAvatar;
    private TextInputEditText etGroupName;
    private TextInputEditText etGroupNotice;
    private Button btnCreateGroup;

    private ActivityResultLauncher<String> imagePickerLauncher;
    private File selectedAvatarFile = null;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.group_creat);

        MaterialToolbar toolbar = findViewById(R.id.create_group_toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        ivGroupAvatar = findViewById(R.id.iv_group_avatar);
        etGroupName = findViewById(R.id.et_group_name);
        etGroupNotice = findViewById(R.id.et_group_notice);
        btnCreateGroup = findViewById(R.id.btn_create_group);

        imagePickerLauncher = registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
            if (uri != null) {
                String fileName = getFileName(uri);
                selectedAvatarFile = uriToFile(uri, fileName);
                if (selectedAvatarFile != null) {
                    Glide.with(this).load(selectedAvatarFile).circleCrop().into(ivGroupAvatar);
                } else {
                    Toast.makeText(this, "图片读取失败", Toast.LENGTH_SHORT).show();
                }
            }
        });

        ivGroupAvatar.setOnClickListener(v -> imagePickerLauncher.launch("image/*"));

        btnCreateGroup.setOnClickListener(v -> createGroup());
    }

    private void createGroup() {
        String groupName = etGroupName.getText() != null ? etGroupName.getText().toString().trim() : "";
        String groupNotice = etGroupNotice.getText() != null ? etGroupNotice.getText().toString().trim() : "";

        if (groupName.isEmpty()) {
            Toast.makeText(this, "群聊名称不能为空", Toast.LENGTH_SHORT).show();
            return;
        }

        int joinType = 1; // default: 管理员同意

        User user = UserDao.getInstance().getUser();
        if (user == null || user.getToken() == null) {
            Toast.makeText(this, "未登录，无法创建群聊", Toast.LENGTH_SHORT).show();
            return;
        }

        btnCreateGroup.setEnabled(false);
        btnCreateGroup.setText("创建中...");

        Map<String, Object> params = new HashMap<>();
        params.put("groupName", groupName);
        params.put("groupNotice", groupNotice);
        params.put("joinType", joinType);

        Map<String, File> files = new HashMap<>();
        if (selectedAvatarFile != null) {
            files.put("avatarFile", selectedAvatarFile);
            files.put("avatarCover", selectedAvatarFile);
        }

        Type type = new TypeToken<Result<Object>>(){}.getType();
        
        HttpClient.uploadFiles("/group/saveGroup", params, files, user.getToken(), type, result -> {
            handleResult(result);
        });
    }

    private void handleResult(Result<Object> result) {
        runOnUiThread(() -> {
            btnCreateGroup.setEnabled(true);
            btnCreateGroup.setText("创建群聊");

            if (result != null && "success".equals(result.getStatus())) {
                Toast.makeText(GroupCreat.this, "创建群聊成功", Toast.LENGTH_SHORT).show();
                finish();
            } else {
                String errorMsg = result != null && result.getInfo() != null ? result.getInfo() : "创建失败，网络或服务器错误";
                Toast.makeText(GroupCreat.this, errorMsg, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private String getFileName(Uri uri) {
        String result = null;
        if (uri.getScheme().equals("content")) {
            try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (index >= 0) result = cursor.getString(index);
                }
            }
        }
        if (result == null) {
            result = uri.getPath();
            int cut = result.lastIndexOf('/');
            if (cut != -1) result = result.substring(cut + 1);
        }
        return result;
    }

    private File uriToFile(Uri uri, String fileName) {
        try {
            InputStream in = getContentResolver().openInputStream(uri);
            if (in == null) return null;
            File tempFile = new File(getCacheDir(), fileName);
            FileOutputStream out = new FileOutputStream(tempFile);
            byte[] buf = new byte[1024];
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
            out.close();
            in.close();
            return tempFile;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
