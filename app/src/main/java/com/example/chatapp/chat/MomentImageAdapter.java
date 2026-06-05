package com.example.chatapp.chat;

import android.content.Context;
import android.content.SharedPreferences;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.chatapp.AppConfig;
import com.example.chatapp.R;

import java.util.List;

public class MomentImageAdapter extends RecyclerView.Adapter<MomentImageAdapter.ImageViewHolder> {

    private final Context context;
    private final List<String> imageUrls;
    private final MomentAdapter.OnMomentClickListener listener;

    public MomentImageAdapter(Context context, List<String> imageUrls, MomentAdapter.OnMomentClickListener listener) {
        this.context = context;
        this.imageUrls = imageUrls;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ImageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ImageView imageView = new ImageView(context);
        
        int screenWidth = context.getResources().getDisplayMetrics().widthPixels;
        int padding = (int) (16 * context.getResources().getDisplayMetrics().density); // 16dp padding
        int availableWidth = screenWidth - padding * 2;
        
        int spanCount = imageUrls.size() == 1 ? 1 : (imageUrls.size() == 4 ? 2 : 3);
        int size = availableWidth / spanCount;
        
        RecyclerView.LayoutParams layoutParams = new RecyclerView.LayoutParams(size, size);
        layoutParams.setMargins(4, 4, 4, 4);
        imageView.setLayoutParams(layoutParams);
        imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        imageView.setBackgroundColor(0xFFEEEEEE);
        
        return new ImageViewHolder(imageView);
    }

    @Override
    public void onBindViewHolder(@NonNull ImageViewHolder holder, int position) {
        String token = com.example.chatapp.UserDao.getInstance().getUser() != null ? com.example.chatapp.UserDao.getInstance().getUser().getToken() : context.getSharedPreferences("ChatApp", Context.MODE_PRIVATE).getString("token", "");
        String url = AppConfig.MOMENT_IMAGE_BASE_URL + imageUrls.get(position);
        com.bumptech.glide.load.model.GlideUrl glideUrl = new com.bumptech.glide.load.model.GlideUrl(url, 
            new com.bumptech.glide.load.model.LazyHeaders.Builder().addHeader("token", token).build());
        
        Glide.with(context)
                .load(glideUrl)
                .placeholder(R.mipmap.ic_launcher_round)
                .error(R.mipmap.ic_launcher_round)
                .into(holder.imageView);

        holder.imageView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onImageClick(imageUrls, position);
            }
        });
    }

    @Override
    public int getItemCount() {
        return imageUrls.size();
    }

    static class ImageViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;

        public ImageViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = (ImageView) itemView;
        }
    }
}
