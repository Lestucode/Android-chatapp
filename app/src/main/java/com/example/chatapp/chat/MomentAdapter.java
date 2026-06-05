package com.example.chatapp.chat;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.chatapp.AppConfig;
import com.example.chatapp.R;
import com.example.chatapp.data.MomentCommentVO;
import com.example.chatapp.data.MomentVO;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MomentAdapter extends RecyclerView.Adapter<MomentAdapter.MomentViewHolder> {

    private final Context context;
    private final List<MomentVO> momentList;
    private final String currentUserId;
    private final OnMomentClickListener listener;

    public interface OnMomentClickListener {
        void onLikeClick(MomentVO moment, int position);
        void onCommentClick(MomentVO moment, int position);
        void onDeleteClick(MomentVO moment, int position);
        void onImageClick(List<String> imageUrls, int position);
    }

    public MomentAdapter(Context context, List<MomentVO> momentList, String currentUserId, OnMomentClickListener listener) {
        this.context = context;
        this.momentList = momentList;
        this.currentUserId = currentUserId;
        this.listener = listener;
    }

    @NonNull
    @Override
    public MomentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_moment, parent, false);
        return new MomentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MomentViewHolder holder, int position) {
        MomentVO moment = momentList.get(position);

        holder.tvNickName.setText(moment.getUserNickName());
        holder.tvTime.setText(moment.getCreateTime());
        holder.tvContent.setText(moment.getContent());

        // Avatar
        String token = com.example.chatapp.UserDao.getInstance().getUser() != null ? com.example.chatapp.UserDao.getInstance().getUser().getToken() : context.getSharedPreferences("ChatApp", Context.MODE_PRIVATE).getString("token", "");
        String avatarUrl = AppConfig.FILE_BASE_URL + moment.getUserId() + "&showCover=true&t=" + System.currentTimeMillis();
        com.bumptech.glide.load.model.GlideUrl glideUrl = new com.bumptech.glide.load.model.GlideUrl(avatarUrl, 
            new com.bumptech.glide.load.model.LazyHeaders.Builder().addHeader("token", token).build());
        Glide.with(context)
                .load(glideUrl)
                .placeholder(R.drawable.dinosaur)
                .error(R.drawable.dinosaur)
                .circleCrop()
                .into(holder.ivAvatar);

        // Delete button visibility
        if (currentUserId != null && currentUserId.equals(moment.getUserId())) {
            holder.ivDelete.setVisibility(View.VISIBLE);
            holder.ivDelete.setOnClickListener(v -> listener.onDeleteClick(moment, position));
        } else {
            holder.ivDelete.setVisibility(View.GONE);
        }

        // Images
        if (moment.getImageUrls() != null && !moment.getImageUrls().isEmpty()) {
            holder.rvImages.setVisibility(View.VISIBLE);
            List<String> images = Arrays.asList(moment.getImageUrls().split(","));
            
            int spanCount = images.size() == 1 ? 1 : (images.size() == 4 ? 2 : 3);
            holder.rvImages.setLayoutManager(new GridLayoutManager(context, spanCount));
            MomentImageAdapter imageAdapter = new MomentImageAdapter(context, images, listener);
            holder.rvImages.setAdapter(imageAdapter);
        } else {
            holder.rvImages.setVisibility(View.GONE);
        }

        // Like & Comment stats
        holder.tvLikeCount.setText(String.valueOf(moment.getLikeCount() != null ? moment.getLikeCount() : 0));
        holder.tvCommentCount.setText(String.valueOf(moment.getCommentCount() != null ? moment.getCommentCount() : 0));

        if (moment.getLiked() != null && moment.getLiked()) {
            holder.ivLike.setColorFilter(Color.parseColor("#FF0000")); // Red
        } else {
            holder.ivLike.setColorFilter(Color.parseColor("#999999")); // Gray
        }

        holder.llLike.setOnClickListener(v -> listener.onLikeClick(moment, position));
        holder.llComment.setOnClickListener(v -> listener.onCommentClick(moment, position));

        // Comments List
        if (moment.getCommentList() != null && !moment.getCommentList().isEmpty()) {
            holder.llComments.setVisibility(View.VISIBLE);
            holder.llComments.removeAllViews();
            for (MomentCommentVO comment : moment.getCommentList()) {
                TextView tvComment = new TextView(context);
                tvComment.setLayoutParams(new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                tvComment.setTextSize(14f);
                tvComment.setPadding(0, 4, 0, 4);

                String nickName = comment.getUserNickName() + ": ";
                String content = comment.getContent();
                SpannableStringBuilder builder = new SpannableStringBuilder(nickName + content);
                
                builder.setSpan(new ForegroundColorSpan(Color.parseColor("#3F51B5")), 0, nickName.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                builder.setSpan(new StyleSpan(android.graphics.Typeface.BOLD), 0, nickName.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                builder.setSpan(new ForegroundColorSpan(Color.parseColor("#333333")), nickName.length(), builder.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                
                tvComment.setText(builder);
                holder.llComments.addView(tvComment);
            }
        } else {
            holder.llComments.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return momentList.size();
    }

    static class MomentViewHolder extends RecyclerView.ViewHolder {
        ImageView ivAvatar, ivDelete, ivLike;
        TextView tvNickName, tvTime, tvContent, tvLikeCount, tvCommentCount;
        RecyclerView rvImages;
        LinearLayout llLike, llComment, llComments;

        public MomentViewHolder(@NonNull View itemView) {
            super(itemView);
            ivAvatar = itemView.findViewById(R.id.ivAvatar);
            ivDelete = itemView.findViewById(R.id.ivDelete);
            tvNickName = itemView.findViewById(R.id.tvNickName);
            tvTime = itemView.findViewById(R.id.tvTime);
            tvContent = itemView.findViewById(R.id.tvContent);
            rvImages = itemView.findViewById(R.id.rvImages);
            llLike = itemView.findViewById(R.id.llLike);
            ivLike = itemView.findViewById(R.id.ivLike);
            tvLikeCount = itemView.findViewById(R.id.tvLikeCount);
            llComment = itemView.findViewById(R.id.llComment);
            tvCommentCount = itemView.findViewById(R.id.tvCommentCount);
            llComments = itemView.findViewById(R.id.llComments);
        }
    }
}
