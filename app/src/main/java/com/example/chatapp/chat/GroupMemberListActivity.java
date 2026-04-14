package com.example.chatapp.chat;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
import com.google.android.material.appbar.MaterialToolbar;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class GroupMemberListActivity extends AppCompatActivity {

    private String groupId;
    private RecyclerView rvMemberList;
    private MemberListAdapter adapter;
    private List<Map<String, Object>> memberList = new ArrayList<>();
    private MaterialToolbar toolbar;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_member_list);

        groupId = getIntent().getStringExtra("groupId");

        toolbar = findViewById(R.id.member_list_toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        rvMemberList = findViewById(R.id.rvMemberList);
        rvMemberList.setLayoutManager(new LinearLayoutManager(this));
        adapter = new MemberListAdapter();
        rvMemberList.setAdapter(adapter);

        loadMembers();
    }

    private void loadMembers() {
        User user = UserDao.getInstance().getUser();
        if (user == null || user.getToken() == null) return;

        Type type = new TypeToken<Result<Map<String, Object>>>(){}.getType();
        HttpClient.get("/group/getGroupInfo4Chat?groupId=" + groupId, type, user.getToken(), result -> {
            if (result != null && "success".equals(result.getStatus())) {
                Map<String, Object> data = result.getDataAs(Map.class);
                if (data != null) {
                    runOnUiThread(() -> {
                        List<Map<String, Object>> members = (List<Map<String, Object>>) data.get("userContactList");
                        if (members != null) {
                            memberList.clear();
                            memberList.addAll(members);
                            adapter.notifyDataSetChanged();
                            
                            toolbar.setTitle("群聊成员 (" + memberList.size() + ")");
                        }
                    });
                }
            } else {
                runOnUiThread(() -> Toast.makeText(GroupMemberListActivity.this, "加载成员失败", Toast.LENGTH_SHORT).show());
            }
        });
    }

    private class MemberListAdapter extends RecyclerView.Adapter<MemberListAdapter.ViewHolder> {

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_group_member_linear, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Map<String, Object> member = memberList.get(position);
            String userId = String.valueOf(member.get("userId"));
            String contactName = member.get("contactName") != null ? String.valueOf(member.get("contactName")) : userId;

            holder.tvName.setText(contactName);

            User user = UserDao.getInstance().getUser();
            if (user != null && user.getToken() != null) {
                String avatarUrl = HttpClient.BASE_URL + "/chat/downloadFile?fileId=" + userId + "&showCover=true&t=" + System.currentTimeMillis();
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
        }

        @Override
        public int getItemCount() {
            return memberList.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            ImageView ivAvatar;
            TextView tvName;

            ViewHolder(View itemView) {
                super(itemView);
                ivAvatar = itemView.findViewById(R.id.ivAvatar);
                tvName = itemView.findViewById(R.id.tvName);
            }
        }
    }
}