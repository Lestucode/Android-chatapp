package com.example.chatapp.chat;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.load.model.LazyHeaders;
import com.example.chatapp.R;
import com.example.chatapp.database.ChatSessionEntity;
import com.example.chatapp.UserDao;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;

public class ChatSessionAdapter extends ListAdapter<ChatSessionEntity, ChatSessionAdapter.SessionViewHolder> {

    private final Context context;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
    private final String token;

    public ChatSessionAdapter(Context context) {
        super(new DiffUtil.ItemCallback<ChatSessionEntity>() {
            @Override
            public boolean areItemsTheSame(@NonNull ChatSessionEntity oldItem, @NonNull ChatSessionEntity newItem) {
                return oldItem.getSessionId().equals(newItem.getSessionId());
            }

            @Override
            public boolean areContentsTheSame(@NonNull ChatSessionEntity oldItem, @NonNull ChatSessionEntity newItem) {
                return Objects.equals(oldItem.getLastReceiveTime(), newItem.getLastReceiveTime()) &&
                        Objects.equals(oldItem.getUnreadCount(), newItem.getUnreadCount()) &&
                        Objects.equals(oldItem.getContactName(), newItem.getContactName()) &&
                        Objects.equals(oldItem.getLastMessage(), newItem.getLastMessage());
            }
        });
        this.context = context;
        String t = null;
        try {
            if (UserDao.getInstance().getUser() != null) {
                t = UserDao.getInstance().getUser().getToken();
            }
        } catch (Exception ignored) {
        }
        this.token = t;
    }

    @NonNull
    @Override
    public SessionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.friend_contact_item, parent, false);
        return new SessionViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SessionViewHolder holder, int position) {
        ChatSessionEntity item = getItem(position);

        holder.friendName.setText(item.getContactName() != null ? item.getContactName() : item.getContactId());
        holder.friendLastMsg.setText(item.getLastMessage() != null ? item.getLastMessage() : "");
        
        if (item.getLastReceiveTime() != null && item.getLastReceiveTime() > 0) {
            holder.friendTime.setText(dateFormat.format(new Date(item.getLastReceiveTime())));
            holder.friendTime.setVisibility(View.VISIBLE);
        } else {
            holder.friendTime.setVisibility(View.GONE);
        }

        Integer unread = item.getUnreadCount();
        if (unread != null && unread > 0) {
            holder.unreadBadge.setText(String.valueOf(unread));
            holder.unreadBadge.setVisibility(View.VISIBLE);
        } else {
            holder.unreadBadge.setVisibility(View.GONE);
        }

        // 加载头像
        String avatarUrl = "http://82.157.200.53:5050/api/chat/downloadFile?fileId=" + item.getContactId() + "&showCover=false";
        if (token != null && !token.isEmpty()) {
            GlideUrl glideUrl = new GlideUrl(avatarUrl, new LazyHeaders.Builder().addHeader("token", token).build());
            Glide.with(context)
                    .load(glideUrl)
                    .placeholder(R.drawable.dinosaur)
                    .circleCrop()
                    .error(R.drawable.dinosaur)
                    .into(holder.friendImg);
        } else {
            Glide.with(context)
                    .load(avatarUrl)
                    .placeholder(R.drawable.dinosaur)
                    .circleCrop()
                    .error(R.drawable.dinosaur)
                    .into(holder.friendImg);
        }

        // 点击跳转到聊天页面
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, ChatActivity.class);
            intent.putExtra("contactId", item.getContactId());
            intent.putExtra("contactName", item.getContactName());
            context.startActivity(intent);
        });
    }

    public static class SessionViewHolder extends RecyclerView.ViewHolder {
        public TextView friendName;
        public TextView friendLastMsg;
        public ImageView friendImg;
        public TextView friendTime;
        public TextView unreadBadge;

        public SessionViewHolder(View itemView) {
            super(itemView);
            // 确保这里的 ID 与您的 friend_contact_item.xml 布局中的 ID 一致！
            friendName = itemView.findViewById(R.id.friend_name);
            friendLastMsg = itemView.findViewById(R.id.friend_last_msg);
            friendImg = itemView.findViewById(R.id.friend_img);
            friendTime = itemView.findViewById(R.id.friend_time);
            unreadBadge = itemView.findViewById(R.id.unread_badge_text);
        }
    }
}
