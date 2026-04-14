//package com.example.chatapp.chat;
//
//import android.content.Context;
//import android.content.Intent;
//import android.view.LayoutInflater;
//import android.view.View;
//import android.view.ViewGroup;
//import android.widget.TextView;
//import android.widget.ImageView;
//import androidx.annotation.NonNull;
//import androidx.recyclerview.widget.RecyclerView;
//
//import com.example.chatapp.R;
//import com.example.chatapp.chat.ConversationItem; // 确保路径正确！
//
//import java.util.List;
//
//public class ChatListAdapter extends RecyclerView.Adapter<ChatListAdapter.ViewHolder> {
//
//    private final List<ConversationItem> dataList;
//    private final Context context;
//
//    public ChatListAdapter(List<ConversationItem> dataList, Context context) {
//        this.dataList = dataList;
//        this.context = context;
//    }
//
//    @NonNull
//    @Override
//    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
//        // 使用您指定的布局文件
//        View view = LayoutInflater.from(context).inflate(R.layout.friend_contact_item, parent, false);
//        return new ViewHolder(view);
//    }
//
//    @Override
//    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
//        ConversationItem item = dataList.get(position);
//
//        // 1. 数据绑定逻辑 (统一使用普通会话的样式，但内容根据状态变化)
//        holder.friendName.setText(item.getFriendName());
//        holder.friendLastMsg.setText(item.getLastMessage());
//
//        // 绑定头像（示例）
//        holder.friendImg.setImageResource(R.drawable.dinosaur); // 统一使用头像
//
//        // 正常显示时间
//        holder.friendTime.setVisibility(View.VISIBLE);
//        // TODO: 绑定正常的头像、时间、未读数等
//
//
//
//
//        // 2. 点击事件逻辑：统一跳转到 ChatActivity
//        holder.itemView.setOnClickListener(v -> {
//            // 【核心修改】所有会话都跳转到 ChatActivity，传递必要的 ID
//            Intent intent = new Intent(context, chatActivity.class);
//            intent.putExtra("FRIEND_ID", item.getFriendId());
//            intent.putExtra("FRIEND_NICKNAME", item.getFriendName());
//            // 确保 ChatActivity 知道这是哪个朋友的会话
//            context.startActivity(intent);
//        });
//    }
//
//    @Override
//    public int getItemCount() {
//        return dataList.size();
//    }
//
//    public static class ViewHolder extends RecyclerView.ViewHolder {
//        public TextView friendName;
//        public TextView friendLastMsg;
//        public ImageView friendImg;
//        public TextView friendTime;
//        public TextView unreadBadge;
//
//        public ViewHolder(View itemView) {
//            super(itemView);
//            // 确保这里的 ID 与您的 rv_chat_list.xml 布局中的 ID 一致！
//            friendName = itemView.findViewById(R.id.friend_name);
//            friendLastMsg = itemView.findViewById(R.id.friend_last_msg);
//            friendImg = itemView.findViewById(R.id.friend_img);
//            friendTime = itemView.findViewById(R.id.friend_time);
//            unreadBadge = itemView.findViewById(R.id.unread_badge_text);
//        }
//    }
//}