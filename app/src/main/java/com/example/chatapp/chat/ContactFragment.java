package com.example.chatapp.chat;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.example.chatapp.R;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.load.model.LazyHeaders;
import com.example.chatapp.User;
import com.example.chatapp.UserDao;
import com.example.chatapp.WebSocketService;
import com.example.chatapp.chat.AddFriendRequest.HttpClient;
import com.example.chatapp.chat.AddFriendRequest.Result;
import com.example.chatapp.data.MessageSendDto;
import com.example.chatapp.data.UserContactSearchResultDto;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import com.google.android.material.tabs.TabLayout;

import java.util.List;
import java.util.ArrayList;
import java.lang.reflect.Type;

public class ContactFragment extends Fragment {
    public static final String ACTION_CONTACTS_DIRTY = "com.example.chatapp.CONTACTS_DIRTY";

    private final Gson gson = new Gson();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final Runnable reloadRunnable = this::loadContacts;

    private RecyclerView rvContacts;
    private TabLayout tabLayoutContacts;
    private final List<UserContactSearchResultDto> contactList = new ArrayList<>();
    private final List<UserContactSearchResultDto> friendList = new ArrayList<>();
    private final List<UserContactSearchResultDto> groupList = new ArrayList<>();
    private ContactListAdapter adapter;
    private int currentTab = 0; // 0 for USER, 1 for GROUP

    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (WebSocketService.ACTION_WS_MESSAGE.equals(action)) {
                String payload = intent.getStringExtra(WebSocketService.EXTRA_MESSAGE);
                if (payload == null) return;
                try {
                    MessageSendDto<?> base = gson.fromJson(payload, MessageSendDto.class);
                    if (base == null || base.getMessageType() == null) return;
                    int mt = base.getMessageType();
                    if (mt == 1 || mt == 10 || mt == 13) {
                        triggerReload();
                    }
                } catch (Exception ignore) {
                }
            } else if (WebSocketService.ACTION_WS_RECONNECTED.equals(action)) {
                triggerReload();
            } else if (ACTION_CONTACTS_DIRTY.equals(action)) {
                triggerReload();
            }
        }
    };

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_contact, container, false);

    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        tabLayoutContacts = view.findViewById(R.id.tab_layout_contacts);
        tabLayoutContacts.addTab(tabLayoutContacts.newTab().setText("好友"));
        tabLayoutContacts.addTab(tabLayoutContacts.newTab().setText("群聊"));

        tabLayoutContacts.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                currentTab = tab.getPosition();
                updateList();
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });

        rvContacts = view.findViewById(R.id.rv_chat_list);
        rvContacts.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new ContactListAdapter(requireContext(), contactList);
        rvContacts.setAdapter(adapter);
        loadContacts();
    }

    private void updateList() {
        contactList.clear();
        if (currentTab == 0) {
            contactList.addAll(friendList);
        } else {
            contactList.addAll(groupList);
        }
        adapter.notifyDataSetChanged();
    }

    @Override
    public void onStart() {
        super.onStart();
        IntentFilter filter = new IntentFilter();
        filter.addAction(WebSocketService.ACTION_WS_MESSAGE);
        filter.addAction(WebSocketService.ACTION_WS_RECONNECTED);
        filter.addAction(ACTION_CONTACTS_DIRTY);
        LocalBroadcastManager.getInstance(requireContext()).registerReceiver(receiver, filter);
    }

    @Override
    public void onStop() {
        super.onStop();
        LocalBroadcastManager.getInstance(requireContext()).unregisterReceiver(receiver);
    }

    private void triggerReload() {
        mainHandler.removeCallbacks(reloadRunnable);
        mainHandler.postDelayed(reloadRunnable, 300);
    }

    private void loadContacts() {
        if (!isAdded() || getContext() == null) return;
        User user = UserDao.getInstance().getUser();
        String token = user != null ? user.getToken() : null;
        if (token == null || token.isEmpty()) return;

        Type type = new TypeToken<Result<Object>>(){}.getType();
        
        // Load Friends
        HttpClient.get("/contact/loadContact?contactType=USER", type, token, result -> {
            if (!isAdded() || getActivity() == null) return;
            getActivity().runOnUiThread(() -> {
                if (result == null || !"success".equals(result.getStatus())) {
                    return;
                }
                List<UserContactSearchResultDto> parsed = parseContacts(result.getData());
                if (parsed != null) {
                    friendList.clear();
                    friendList.addAll(parsed);
                    if (currentTab == 0) updateList();
                    
                    // 同步更新本地聊天会话列表的昵称
                    com.example.chatapp.util.AppExecutors.io().execute(() -> {
                        com.example.chatapp.database.ChatSessionDao dao = com.example.chatapp.database.AppDatabase.getInstance(requireContext()).chatSessionDao();
                        for (UserContactSearchResultDto c : parsed) {
                            String name = c.getContactName();
                            if (name == null || name.isEmpty()) name = c.getContactId();
                            dao.updateContactName(c.getContactId(), name);
                        }
                    });
                }
            });
        });

        // Load Groups
        HttpClient.get("/contact/loadContact?contactType=GROUP", type, token, result -> {
            if (!isAdded() || getActivity() == null) return;
            getActivity().runOnUiThread(() -> {
                if (result == null || !"success".equals(result.getStatus())) {
                    return;
                }
                List<UserContactSearchResultDto> parsed = parseContacts(result.getData());
                if (parsed != null) {
                    groupList.clear();
                    groupList.addAll(parsed);
                    if (currentTab == 1) updateList();
                    
                    // 同步更新群聊会话列表名称
                    com.example.chatapp.util.AppExecutors.io().execute(() -> {
                        com.example.chatapp.database.ChatSessionDao dao = com.example.chatapp.database.AppDatabase.getInstance(requireContext()).chatSessionDao();
                        for (UserContactSearchResultDto c : parsed) {
                            String name = c.getContactName();
                            if (name == null || name.isEmpty()) name = c.getContactId();
                            dao.updateContactName(c.getContactId(), name);
                        }
                    });
                }
            });
        });
    }

    @Nullable
    private List<UserContactSearchResultDto> parseContacts(Object data) {
        if (data == null) return null;
        try {
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
            Type listType = new TypeToken<List<UserContactSearchResultDto>>(){}.getType();
            return gson.fromJson(arr, listType);
        } catch (Exception e) {
            return null;
        }
    }

    private static class ContactListAdapter extends RecyclerView.Adapter<ContactListAdapter.VH> {
        private final Context context;
        private final List<UserContactSearchResultDto> data;

        private ContactListAdapter(Context context, List<UserContactSearchResultDto> data) {
            this.context = context;
            this.data = data;
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(context).inflate(R.layout.friend_contact_item, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            UserContactSearchResultDto item = data.get(position);
            
            // 使用 contactName 显示，如果为空再兜底用 contactId
            String name = item.getContactName();
            if (name == null || name.isEmpty()) {
                name = item.getContactId();
            }
            holder.tvName.setText(name != null ? name : "");

            User me = UserDao.getInstance().getUser();
            String token = me != null ? me.getToken() : "";
            String contactId = item.getContactId();
            long t = item.getAvatarLastUpdate() != null ? item.getAvatarLastUpdate() : System.currentTimeMillis();
            String avatarUrl = HttpClient.BASE_URL + "/chat/downloadFile?fileId=" + contactId + "&showCover=true&t=" + t;
            GlideUrl glideUrl = new GlideUrl(avatarUrl, new LazyHeaders.Builder()
                    .addHeader("token", token)
                    .build());
            Glide.with(context)
                    .load(glideUrl)
                    .circleCrop()
                    .placeholder(R.drawable.dinosaur)
                    .error(R.drawable.dinosaur)
                    .into(holder.ivAvatar);

            holder.itemView.setOnClickListener(v -> {
                Intent intent = new Intent(context, ChatActivity.class);
                intent.putExtra("contactId", contactId);
                intent.putExtra("contactName", holder.tvName.getText().toString());
                context.startActivity(intent);
            });
        }

        @Override
        public int getItemCount() {
            return data != null ? data.size() : 0;
        }

        static class VH extends RecyclerView.ViewHolder {
            com.google.android.material.imageview.ShapeableImageView ivAvatar;
            TextView tvName;

            VH(@NonNull View itemView) {
                super(itemView);
                ivAvatar = itemView.findViewById(R.id.friend_img);
                tvName = itemView.findViewById(R.id.friend_name);
                tvName.setGravity(Gravity.CENTER_VERTICAL);
            }
        }
    }

}
