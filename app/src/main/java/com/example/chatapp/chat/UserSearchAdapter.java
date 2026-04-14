package com.example.chatapp.chat;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.load.model.LazyHeaders;
import com.example.chatapp.R;
import com.example.chatapp.chat.AddFriendRequest.HttpClient;
import com.example.chatapp.chat.AddFriendRequest.Result;
import com.example.chatapp.data.UserContactSearchResultDto;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UserSearchAdapter extends RecyclerView.Adapter<UserSearchAdapter.UserViewHolder> {

    private List<UserContactSearchResultDto> userList;   // 搜索到的用户列表
    private Context context;             // 上下文
    private String jwt;                  // 登录 token

    // 构造方法：传入数据和上下文
    public UserSearchAdapter(List<UserContactSearchResultDto> userList, Context context, String jwt) {
        this.userList = userList;
        this.context = context;
        this.jwt = jwt;
    }

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(context)
                .inflate(R.layout.item_search_result_friend, parent, false);
        return new UserViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        UserContactSearchResultDto user = userList.get(position);

        String showName = (user.getNickName() != null && !user.getNickName().isEmpty())
                ? user.getNickName()
                : user.getContactId();
        holder.tvFriendName.setText(showName);

        // 加载头像
        if (user.getContactId() != null && !user.getContactId().isEmpty()) {
            String avatarUrl = "http://82.157.200.53:5050/api/chat/downloadFile?fileId=" + user.getContactId() + "&showCover=true";
            GlideUrl glideUrl = new GlideUrl(avatarUrl, new LazyHeaders.Builder()
                    .addHeader("token", jwt)
                    .build());

            Glide.with(context)
                    .load(glideUrl)
                    .placeholder(R.drawable.dinosaur)
                    .error(R.drawable.dinosaur)
                    .circleCrop()
                    .into(holder.sivAvatar);
        } else {
            holder.sivAvatar.setImageResource(R.drawable.dinosaur);
        }

        // 添加好友按钮点击事件
        holder.btnAdd.setOnClickListener(v -> {
            String contactId = user.getContactId();
            String contactType = user.getContactType(); // "USER" 或者 "GROUP"
            String applyInfo = "你好，我想添加你为好友"; // 可以在这里提供一个输入框获取

            Map<String, Object> params = new HashMap<>();
            params.put("contactId", contactId);
            params.put("contactType", contactType);
            params.put("applyInfo", applyInfo);

            Type type = new TypeToken<Result<Integer>>(){}.getType();
            HttpClient.post("/contact/applyAdd", params, jwt, type, result -> {
                new Handler(Looper.getMainLooper()).post(() -> {
                    if (result != null && "success".equals(result.getStatus())) {
                        Toast.makeText(context, "已向 " + showName + " 发起好友请求", Toast.LENGTH_SHORT).show();
                        holder.btnAdd.setEnabled(false);
                        holder.btnAdd.setText("已申请");
                    } else {
                        String msg = result != null ? result.getInfo() : "请求失败，服务器返回错误";
                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show();
                    }
                });
            });
        });
    }

    @Override
    public int getItemCount() {
        return userList.size();
    }

    // ViewHolder
    public static class UserViewHolder extends RecyclerView.ViewHolder {
        ShapeableImageView sivAvatar; // 头像
        TextView tvFriendName;        // 昵称
        MaterialButton btnAdd;        // 添加按钮

        public UserViewHolder(@NonNull View itemView) {
            super(itemView);
            sivAvatar = itemView.findViewById(R.id.siv_friend_avatar);
            tvFriendName = itemView.findViewById(R.id.tv_friend_name);
            btnAdd = itemView.findViewById(R.id.btn_send_request);
        }
    }
}
