package com.example.chatapp.chat;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.load.model.LazyHeaders;
import com.example.chatapp.R;
import com.example.chatapp.User;
import com.example.chatapp.UserDao;
import com.example.chatapp.chat.AddFriendRequest.HttpClient;
import com.example.chatapp.chat.AddFriendRequest.Result;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class InviteFriendActivity extends AppCompatActivity {

    private String groupId;
    private List<String> existingMemberIds;
    private RecyclerView rvFriends;
    private FriendAdapter adapter;
    private List<Map<String, Object>> friendList = new ArrayList<>();
    private Set<String> selectedIds = new HashSet<>();
    private Button btnInvite;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_invite_friend);

        groupId = getIntent().getStringExtra("groupId");
        existingMemberIds = getIntent().getStringArrayListExtra("memberIds");
        if (existingMemberIds == null) {
            existingMemberIds = new ArrayList<>();
        }

        findViewById(R.id.tvCancel).setOnClickListener(v -> finish());
        btnInvite = findViewById(R.id.btnInvite);
        btnInvite.setOnClickListener(v -> performInvite());

        rvFriends = findViewById(R.id.rvFriends);
        rvFriends.setLayoutManager(new LinearLayoutManager(this));
        adapter = new FriendAdapter();
        rvFriends.setAdapter(adapter);

        loadFriends();
    }

    private void updateInviteButton() {
        if (selectedIds.isEmpty()) {
            btnInvite.setText("立即邀请");
            btnInvite.setEnabled(false);
            btnInvite.setAlpha(0.5f);
        } else {
            btnInvite.setText("立即邀请 (" + selectedIds.size() + ")");
            btnInvite.setEnabled(true);
            btnInvite.setAlpha(1.0f);
        }
    }

    private void loadFriends() {
        User user = UserDao.getInstance().getUser();
        if (user == null || user.getToken() == null) return;

        Type type = new TypeToken<Result<Object>>(){}.getType();
        HttpClient.get("/contact/loadContact?contactType=USER", type, user.getToken(), result -> {
            runOnUiThread(() -> {
                if (result != null && "success".equals(result.getStatus())) {
                    List<Map<String, Object>> allFriends = parseContacts(result.getData());
                    if (allFriends != null) {
                        friendList.clear();
                        for (Map<String, Object> f : allFriends) {
                            String fId = String.valueOf(f.get("contactId"));
                            if (!existingMemberIds.contains(fId)) {
                                friendList.add(f);
                            }
                        }
                        adapter.notifyDataSetChanged();
                    }
                } else {
                    Toast.makeText(this, "获取好友列表失败", Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    private List<Map<String, Object>> parseContacts(Object data) {
        if (data == null) return null;
        try {
            Gson gson = new Gson();
            JsonElement root = gson.toJsonTree(data);
            JsonArray arr = null;
            if (root.isJsonArray()) {
                arr = root.getAsJsonArray();
            } else if (root.isJsonObject()) {
                JsonObject obj = root.getAsJsonObject();
                JsonElement listEl = obj.get("list");
                if (listEl != null && listEl.isJsonArray()) {
                    arr = listEl.getAsJsonArray();
                }
            }
            if (arr == null) return null;
            Type listType = new TypeToken<List<Map<String, Object>>>(){}.getType();
            return gson.fromJson(arr, listType);
        } catch (Exception e) {
            return null;
        }
    }

    private void performInvite() {
        if (selectedIds.isEmpty()) return;
        
        User user = UserDao.getInstance().getUser();
        if (user == null || user.getToken() == null) return;

        Map<String, Object> params = new HashMap<>();
        params.put("groupId", groupId);
        
        StringBuilder sb = new StringBuilder();
        int i = 0;
        for (String id : selectedIds) {
            sb.append(id);
            if (i < selectedIds.size() - 1) sb.append(",");
            i++;
        }
        params.put("selectContacts", sb.toString());
        params.put("opType", 1); // 1 is Add

        Type type = new TypeToken<Result<Object>>(){}.getType();
        HttpClient.post("/group/addOrRemoveGroupUser", params, user.getToken(), type, result -> {
            runOnUiThread(() -> {
                if (result != null && "success".equals(result.getStatus())) {
                    Toast.makeText(this, "邀请成功", Toast.LENGTH_SHORT).show();
                    setResult(RESULT_OK);
                    finish();
                } else {
                    String error = result != null && result.getInfo() != null ? result.getInfo() : "邀请失败";
                    Toast.makeText(this, error, Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    private class FriendAdapter extends RecyclerView.Adapter<FriendAdapter.ViewHolder> {

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_invite_friend, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Map<String, Object> friend = friendList.get(position);
            String contactId = String.valueOf(friend.get("contactId"));
            String contactName = friend.get("contactName") != null ? String.valueOf(friend.get("contactName")) : contactId;

            holder.tvName.setText(contactName);
            holder.cbSelect.setChecked(selectedIds.contains(contactId));

            User user = UserDao.getInstance().getUser();
            if (user != null && user.getToken() != null) {
                String avatarUrl = HttpClient.BASE_URL + "/chat/downloadFile?fileId=" + contactId + "&showCover=true&t=" + System.currentTimeMillis();
                GlideUrl glideUrl = new GlideUrl(avatarUrl, new LazyHeaders.Builder()
                        .addHeader("token", user.getToken())
                        .build());
                Glide.with(holder.itemView.getContext())
                        .load(glideUrl)
                        .circleCrop()
                        .placeholder(R.drawable.dinosaur)
                        .error(R.drawable.dinosaur)
                        .into(holder.ivAvatar);
            }

            holder.itemView.setOnClickListener(v -> {
                if (selectedIds.contains(contactId)) {
                    selectedIds.remove(contactId);
                    holder.cbSelect.setChecked(false);
                } else {
                    selectedIds.add(contactId);
                    holder.cbSelect.setChecked(true);
                }
                updateInviteButton();
            });
        }

        @Override
        public int getItemCount() {
            return friendList.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            CheckBox cbSelect;
            ImageView ivAvatar;
            TextView tvName;

            ViewHolder(View itemView) {
                super(itemView);
                cbSelect = itemView.findViewById(R.id.cbSelect);
                ivAvatar = itemView.findViewById(R.id.ivAvatar);
                tvName = itemView.findViewById(R.id.tvName);
            }
        }
    }
}
