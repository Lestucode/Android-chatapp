package com.example.chatapp.chat;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.load.model.LazyHeaders;
import com.example.chatapp.chat.AddFriendRequest.HttpClient;
import com.example.chatapp.R;
import com.example.chatapp.UserDao;
import com.example.chatapp.chat.AddFriendRequest.Result;
import com.example.chatapp.data.UserContactApplyDto;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NewFriendAdapter extends RecyclerView.Adapter<NewFriendAdapter.ViewHolder> {
    private Context context;
    private List<UserContactApplyDto> applyList;

    public NewFriendAdapter(Context context, List<UserContactApplyDto> applyList) {
        this.context = context;
        this.applyList = applyList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_friend_request, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        UserContactApplyDto apply = applyList.get(position);
        holder.tvNickname.setText(apply.getContactName());
        holder.tvApplyInfo.setText(apply.getApplyInfo() != null && !apply.getApplyInfo().isEmpty() ? apply.getApplyInfo() : "向你发起好友申请");

        // 携带 token 加载申请人头像
        String token = UserDao.getInstance().getUser() != null ? UserDao.getInstance().getUser().getToken() : "";
        String avatarUrl = HttpClient.BASE_URL + "/chat/downloadFile?fileId=" + apply.getApplyUserId() + "&showCover=true&t=" + System.currentTimeMillis();
        GlideUrl glideUrl = new GlideUrl(avatarUrl, new LazyHeaders.Builder()
                .addHeader("token", token)
                .build());
        Glide.with(context)
                .load(glideUrl)
                .circleCrop()
                .placeholder(R.drawable.dinosaur) // Fallback
                .error(R.drawable.dinosaur)
                .into(holder.ivAvatar);

        // Status handling (0: 待处理 1: 已同意 2: 已拒绝 3: 已拉黑)
        Integer status = apply.getStatus();
        if (status != null && status == 0) {
            holder.btnAgree.setVisibility(View.VISIBLE);
            holder.tvStatus.setVisibility(View.GONE);
            holder.btnAgree.setOnClickListener(v -> dealWithApply(apply, 1, position)); // 1 for Agree
        } else {
            holder.btnAgree.setVisibility(View.GONE);
            holder.tvStatus.setVisibility(View.VISIBLE);
            if (status != null && status == 1) {
                holder.tvStatus.setText("已同意");
            } else if (status != null && status == 2) {
                holder.tvStatus.setText("已拒绝");
            } else if (status != null && status == 3) {
                holder.tvStatus.setText("已拉黑");
            } else {
                holder.tvStatus.setText(apply.getStatusName());
            }
        }
    }

    @Override
    public int getItemCount() {
        return applyList == null ? 0 : applyList.size();
    }

    private void dealWithApply(UserContactApplyDto apply, int status, int position) {
        String token = UserDao.getInstance().getUser() != null ? UserDao.getInstance().getUser().getToken() : "";
        Map<String, Object> params = new HashMap<>();
        params.put("applyId", apply.getApplyId());
        params.put("status", status);

        Type type = new TypeToken<Result<Object>>(){}.getType();
        HttpClient.post("/contact/dealWithApply", params, token, type, result -> {
            if (context == null) return;
            if (result != null && "success".equals(result.getStatus())) {
                Toast.makeText(context, "操作成功", Toast.LENGTH_SHORT).show();
                apply.setStatus(status);
                notifyItemChanged(position);
                if (status == 1) {
                    LocalBroadcastManager.getInstance(context).sendBroadcast(new Intent(ContactFragment.ACTION_CONTACTS_DIRTY));
                }
            } else {
                String msg = result != null ? result.getInfo() : "请求失败";
                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show();
            }
        });
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ShapeableImageView ivAvatar;
        TextView tvNickname;
        TextView tvApplyInfo;
        MaterialButton btnAgree;
        TextView tvStatus;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivAvatar = itemView.findViewById(R.id.my_avater);
            tvNickname = itemView.findViewById(R.id.nickname);
            tvApplyInfo = itemView.findViewById(R.id.tv_apply_info);
            btnAgree = itemView.findViewById(R.id.btn_agree);
            tvStatus = itemView.findViewById(R.id.tv_status);
        }
    }
}
