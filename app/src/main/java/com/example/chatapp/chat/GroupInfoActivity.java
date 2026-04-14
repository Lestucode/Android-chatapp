package com.example.chatapp.chat;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
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
import com.example.chatapp.database.AppDatabase;
import com.example.chatapp.util.AppExecutors;
import com.example.chatapp.util.ChatSessionIdUtils;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.Gson;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GroupInfoActivity extends AppCompatActivity {
    
    private String targetGroupId;
    private RecyclerView rvGroupMembers;
    private MemberAdapter adapter;
    private List<Map<String, Object>> memberList = new ArrayList<>();
    
    private Button btnLeaveGroup;
    
    private boolean isOwner = false;
    private String myUserId;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_info);

        targetGroupId = getIntent().getStringExtra("groupId");
        
        User user = UserDao.getInstance().getUser();
        if (user != null) {
            myUserId = user.getUserId();
        }

        MaterialToolbar toolbar = findViewById(R.id.group_info_toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());

        rvGroupMembers = findViewById(R.id.rvGroupMembers);
        rvGroupMembers.setLayoutManager(new androidx.recyclerview.widget.GridLayoutManager(this, 5));
        adapter = new MemberAdapter();
        rvGroupMembers.setAdapter(adapter);

        btnLeaveGroup = findViewById(R.id.btnLeaveGroup);

        btnLeaveGroup.setOnClickListener(v -> showLeaveGroupDialog());

        View llMemberHeader = findViewById(R.id.llMemberHeader);
        llMemberHeader.setOnClickListener(v -> {
            Intent intent = new Intent(GroupInfoActivity.this, GroupMemberListActivity.class);
            intent.putExtra("groupId", targetGroupId);
            startActivity(intent);
        });

        loadGroupInfo();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 100 && resultCode == RESULT_OK) {
            loadGroupInfo();
        }
    }

    private void loadGroupInfo() {
        if (targetGroupId == null) return;
        
        User user = UserDao.getInstance().getUser();
        if (user == null || user.getToken() == null) return;

        Type type = new TypeToken<Result<Map<String, Object>>>(){}.getType();
        HttpClient.get("/group/getGroupInfo4Chat?groupId=" + targetGroupId, type, user.getToken(), result -> {
            if (result != null && "success".equals(result.getStatus())) {
                Map<String, Object> data = result.getDataAs(Map.class);
                if (data != null) {
                    runOnUiThread(() -> {
                        Map<String, Object> groupInfo = (Map<String, Object>) data.get("groupInfo");
                        if (groupInfo != null) {
                            TextView tvGroupName = findViewById(R.id.tvGroupName);
                            TextView tvGroupId = findViewById(R.id.tvGroupId);
                            TextView tvGroupNotice = findViewById(R.id.tvGroupNotice);
                            ImageView ivGroupAvatar = findViewById(R.id.ivGroupAvatar);

                            tvGroupName.setText(groupInfo.get("groupName") != null ? String.valueOf(groupInfo.get("groupName")) : targetGroupId);
                            tvGroupId.setText("群聊号: " + targetGroupId);
                            
                            String notice = groupInfo.get("groupNotice") != null ? String.valueOf(groupInfo.get("groupNotice")) : "暂无公告";
                            tvGroupNotice.setText(notice);

                            String avatarUrl = HttpClient.BASE_URL + "/chat/downloadFile?fileId=" + targetGroupId + "&showCover=true&t=" + System.currentTimeMillis();
                            GlideUrl glideUrl = new GlideUrl(avatarUrl, new LazyHeaders.Builder()
                                    .addHeader("token", user.getToken())
                                    .build());
                            Glide.with(this)
                                    .load(glideUrl)
                                    .circleCrop()
                                    .placeholder(R.drawable.dinosaur)
                                    .error(R.drawable.dinosaur)
                                    .into(ivGroupAvatar);
                            
                            Object ownerIdObj = groupInfo.get("groupOwnerId");
                            if (ownerIdObj != null && String.valueOf(ownerIdObj).equals(myUserId)) {
                                isOwner = true;
                                btnLeaveGroup.setText("解散群聊");
                            } else {
                                isOwner = false;
                                btnLeaveGroup.setText("退出群聊");
                            }
                        }

                        List<Map<String, Object>> members = (List<Map<String, Object>>) data.get("userContactList");
                        if (members != null) {
                            memberList.clear();
                            memberList.addAll(members);
                            adapter.notifyDataSetChanged();
                            
                            TextView tvMemberCount = findViewById(R.id.tvMemberCount);
                            tvMemberCount.setText("群成员 (" + memberList.size() + ")");
                        }
                    });
                }
            } else {
                runOnUiThread(() -> Toast.makeText(GroupInfoActivity.this, "加载群聊信息失败", Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void showAddMemberDialog() {
        User user = UserDao.getInstance().getUser();
        if (user == null || user.getToken() == null) return;

        Type type = new TypeToken<Result<Object>>(){}.getType();
        HttpClient.get("/contact/loadContact?contactType=USER", type, user.getToken(), result -> {
            runOnUiThread(() -> {
                if (result != null && "success".equals(result.getStatus())) {
                    List<Map<String, Object>> allFriends = parseContacts(result.getData());
                    if (allFriends == null || allFriends.isEmpty()) {
                        Toast.makeText(this, "没有好友可以添加", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // Filter out already in group
                    List<Map<String, Object>> addableFriends = new ArrayList<>();
                    for (Map<String, Object> f : allFriends) {
                        String fId = String.valueOf(f.get("contactId"));
                        boolean inGroup = false;
                        for (Map<String, Object> m : memberList) {
                            String mId = String.valueOf(m.get("userId"));
                            if (fId.equals(mId)) {
                                inGroup = true;
                                break;
                            }
                        }
                        if (!inGroup) {
                            addableFriends.add(f);
                        }
                    }

                    if (addableFriends.isEmpty()) {
                        Toast.makeText(this, "所有好友都已经在群里了", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    String[] names = new String[addableFriends.size()];
                    boolean[] checkedItems = new boolean[addableFriends.size()];
                    for (int i = 0; i < addableFriends.size(); i++) {
                        Map<String, Object> f = addableFriends.get(i);
                        String contactId = String.valueOf(f.get("contactId"));
                        String name = f.get("contactName") != null ? String.valueOf(f.get("contactName")) : contactId;
                        names[i] = name;
                        checkedItems[i] = false;
                    }

                    new AlertDialog.Builder(this)
                            .setTitle("添加成员")
                            .setMultiChoiceItems(names, checkedItems, (dialog, which, isChecked) -> checkedItems[which] = isChecked)
                            .setPositiveButton("添加", (dialog, which) -> {
                                List<String> selectedIds = new ArrayList<>();
                                for (int i = 0; i < checkedItems.length; i++) {
                                    if (checkedItems[i]) {
                                        selectedIds.add(String.valueOf(addableFriends.get(i).get("contactId")));
                                    }
                                }
                                if (!selectedIds.isEmpty()) {
                                    performAddOrRemove(selectedIds, 1);
                                }
                            })
                            .setNegativeButton("取消", null)
                            .show();
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

    private void showRemoveMemberDialog() {
        if (memberList == null || memberList.isEmpty()) return;
        
        List<Map<String, Object>> removableMembers = new ArrayList<>();
        for (Map<String, Object> member : memberList) {
            String userId = String.valueOf(member.get("userId"));
            if (!userId.equals(myUserId)) {
                removableMembers.add(member);
            }
        }
        
        if (removableMembers.isEmpty()) {
            Toast.makeText(this, "没有可移除的成员", Toast.LENGTH_SHORT).show();
            return;
        }

        String[] names = new String[removableMembers.size()];
        boolean[] checkedItems = new boolean[removableMembers.size()];
        for (int i = 0; i < removableMembers.size(); i++) {
            Map<String, Object> m = removableMembers.get(i);
            String userId = String.valueOf(m.get("userId"));
            String name = m.get("contactName") != null ? String.valueOf(m.get("contactName")) : userId;
            names[i] = name;
            checkedItems[i] = false;
        }

        new AlertDialog.Builder(this)
                .setTitle("移除成员")
                .setMultiChoiceItems(names, checkedItems, (dialog, which, isChecked) -> checkedItems[which] = isChecked)
                .setPositiveButton("移除", (dialog, which) -> {
                    List<String> selectedIds = new ArrayList<>();
                    for (int i = 0; i < checkedItems.length; i++) {
                        if (checkedItems[i]) {
                            selectedIds.add(String.valueOf(removableMembers.get(i).get("userId")));
                        }
                    }
                    if (!selectedIds.isEmpty()) {
                        performAddOrRemove(selectedIds, 0);
                    }
                })
                .setNegativeButton("取消", null)
                .show();
    }

    private void performAddOrRemove(List<String> selectedIds, int opType) {
        User user = UserDao.getInstance().getUser();
        if (user == null || user.getToken() == null) return;

        Map<String, Object> params = new HashMap<>();
        params.put("groupId", targetGroupId);
        
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < selectedIds.size(); i++) {
            sb.append(selectedIds.get(i));
            if (i < selectedIds.size() - 1) sb.append(",");
        }
        params.put("selectContacts", sb.toString());
        params.put("opType", opType);

        Type type = new TypeToken<Result<Object>>(){}.getType();
        HttpClient.post("/group/addOrRemoveGroupUser", params, user.getToken(), type, result -> {
            runOnUiThread(() -> {
                if (result != null && "success".equals(result.getStatus())) {
                    Toast.makeText(this, "操作成功", Toast.LENGTH_SHORT).show();
                    loadGroupInfo(); // reload list
                } else {
                    String error = result != null && result.getInfo() != null ? result.getInfo() : "操作失败";
                    Toast.makeText(this, error, Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    private void showLeaveGroupDialog() {
        String title = isOwner ? "解散群聊" : "退出群聊";
        String message = isOwner ? "确定要解散该群聊吗？操作不可恢复。" : "确定要退出该群聊吗？";
        
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton("确定", (dialog, which) -> performLeaveOrDissolve())
                .setNegativeButton("取消", null)
                .show();
    }

    private void performLeaveOrDissolve() {
        User user = UserDao.getInstance().getUser();
        if (user == null || user.getToken() == null) return;
        
        String url = isOwner ? "/group/dissolutionGroup" : "/group/leaveGroup";
        Map<String, Object> params = new HashMap<>();
        params.put("groupId", targetGroupId);
        
        Type type = new TypeToken<Result<Object>>(){}.getType();
        HttpClient.post(url, params, user.getToken(), type, result -> {
            runOnUiThread(() -> {
                if (result != null && "success".equals(result.getStatus())) {
                    Toast.makeText(this, isOwner ? "已解散群聊" : "已退出群聊", Toast.LENGTH_SHORT).show();
                    
                    // Clear local DB data
                    AppExecutors.io().execute(() -> {
                        AppDatabase db = AppDatabase.getInstance(this);
                        String sessionId = ChatSessionIdUtils.getChatSessionId4Group(targetGroupId);
                        db.chatSessionDao().deleteSessionById(sessionId);
                        db.chatMessageDao().deleteMessagesBySessionId(sessionId);
                        
                        // Notify contact fragment to reload
                        Intent intent = new Intent("com.example.chatapp.CONTACTS_DIRTY");
                        androidx.localbroadcastmanager.content.LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
                    });

                    // Go back to ChatHomePage and clear ChatActivity
                    Intent intent = new Intent(this, ChatHomePage.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    startActivity(intent);
                    finish();
                } else {
                    String error = result != null && result.getInfo() != null ? result.getInfo() : "操作失败";
                    Toast.makeText(this, error, Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    private class MemberAdapter extends RecyclerView.Adapter<MemberAdapter.ViewHolder> {

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_group_member, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            int maxMembers = isOwner ? 13 : 15;
            int membersToDisplay = Math.min(memberList.size(), maxMembers);
            
            if (position < membersToDisplay) {
                Map<String, Object> member = memberList.get(position);
                String userId = String.valueOf(member.get("userId"));
                String contactName = member.get("contactName") != null ? String.valueOf(member.get("contactName")) : userId;

                holder.tvMemberName.setText(contactName);

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
                            .into(holder.ivMemberAvatar);
                }
                holder.itemView.setOnClickListener(null);
            } else if (isOwner && position == membersToDisplay) {
                holder.tvMemberName.setText("邀请");
                Glide.with(holder.itemView.getContext()).clear(holder.ivMemberAvatar);
                holder.ivMemberAvatar.setImageResource(R.drawable.ic_invite_member);
                holder.itemView.setOnClickListener(v -> {
                    Intent intent = new Intent(GroupInfoActivity.this, InviteFriendActivity.class);
                    intent.putExtra("groupId", targetGroupId);
                    
                    ArrayList<String> memberIds = new ArrayList<>();
                    for (Map<String, Object> m : memberList) {
                        memberIds.add(String.valueOf(m.get("userId")));
                    }
                    intent.putStringArrayListExtra("memberIds", memberIds);
                    startActivityForResult(intent, 100);
                });
            } else if (isOwner) {
                holder.tvMemberName.setText("移除");
                Glide.with(holder.itemView.getContext()).clear(holder.ivMemberAvatar);
                holder.ivMemberAvatar.setImageResource(R.drawable.ic_remove_member);
                holder.itemView.setOnClickListener(v -> showRemoveMemberDialog());
            }
        }

        @Override
        public int getItemCount() {
            int maxMembers = isOwner ? 13 : 15;
            int membersToDisplay = Math.min(memberList.size(), maxMembers);
            return membersToDisplay + (isOwner ? 2 : 0);
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            ImageView ivMemberAvatar;
            TextView tvMemberName;

            ViewHolder(View itemView) {
                super(itemView);
                ivMemberAvatar = itemView.findViewById(R.id.ivMemberAvatar);
                tvMemberName = itemView.findViewById(R.id.tvMemberName);
            }
        }
    }
}