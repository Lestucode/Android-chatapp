package com.example.chatapp.chat;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
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
import com.example.chatapp.database.ChatMessageEntity;

import java.util.Objects;

import androidx.core.content.FileProvider;
import java.io.File;

public class MessageAdapter extends ListAdapter<ChatMessageEntity, RecyclerView.ViewHolder> {
    private Context context;
    private String currentUserId;
    private String contactId;
    private String token;

    public MessageAdapter(Context context, String currentUserId, String contactId, String token) {
        super(new DiffUtil.ItemCallback<ChatMessageEntity>() {
            @Override
            public boolean areItemsTheSame(@NonNull ChatMessageEntity oldItem, @NonNull ChatMessageEntity newItem) {
                return oldItem.getLocalId().equals(newItem.getLocalId());
            }

            @Override
            public boolean areContentsTheSame(@NonNull ChatMessageEntity oldItem, @NonNull ChatMessageEntity newItem) {
                return Objects.equals(oldItem.getStatus(), newItem.getStatus()) &&
                        Objects.equals(oldItem.getMessageContent(), newItem.getMessageContent()) &&
                        Objects.equals(oldItem.getMessageId(), newItem.getMessageId());
            }
        });
        this.context = context;
        this.currentUserId = currentUserId;
        this.contactId = contactId;
        this.token = token;
    }

    private static final int TYPE_ME = 1;
    private static final int TYPE_OTHER = 2;

    @Override
    public int getItemViewType(int position) {
        ChatMessageEntity msg = getItem(position);
        if (currentUserId != null && currentUserId.equals(msg.getSendUserId())) {
            return TYPE_ME;
        }
        return TYPE_OTHER;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == TYPE_ME) {
            View view = inflater.inflate(R.layout.item_message_me, parent, false);
            return new MeViewHolder(view);
        } else {
            View view = inflater.inflate(R.layout.item_message_other, parent, false);
            return new OtherViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ChatMessageEntity msg = getItem(position);
        
        TextView tvMessage;
        View flMedia, llFile;
        ImageView ivMedia, ivPlay;
        TextView tvFileName, tvFileSize;
        
        if (holder instanceof MeViewHolder) {
            MeViewHolder meHolder = (MeViewHolder) holder;
            tvMessage = meHolder.tvMessage;
            flMedia = meHolder.flMedia;
            llFile = meHolder.llFile;
            ivMedia = meHolder.ivMedia;
            ivPlay = meHolder.ivPlay;
            tvFileName = meHolder.tvFileName;
            tvFileSize = meHolder.tvFileSize;
        } else {
            OtherViewHolder otherHolder = (OtherViewHolder) holder;
            tvMessage = otherHolder.tvMessage;
            flMedia = otherHolder.flMedia;
            llFile = otherHolder.llFile;
            ivMedia = otherHolder.ivMedia;
            ivPlay = otherHolder.ivPlay;
            tvFileName = otherHolder.tvFileName;
            tvFileSize = otherHolder.tvFileSize;
            
            // 加载对方头像
            String avatarUrl = "http://82.157.200.53:5050/api/chat/downloadFile?fileId=" + msg.getSendUserId() + "&showCover=false";
            if (token != null && !token.isEmpty()) {
                GlideUrl glideUrl = new GlideUrl(avatarUrl, new LazyHeaders.Builder().addHeader("token", token).build());
                Glide.with(context)
                        .load(glideUrl)
                        .circleCrop()
                        .placeholder(R.drawable.dinosaur)
                        .error(R.drawable.dinosaur)
                        .into(otherHolder.ivAvatar);
            } else {
                Glide.with(context)
                        .load(avatarUrl)
                        .circleCrop()
                        .placeholder(R.drawable.dinosaur)
                        .error(R.drawable.dinosaur)
                        .into(otherHolder.ivAvatar);
            }
        }
        
        // 重置状态
        tvMessage.setVisibility(View.GONE);
        flMedia.setVisibility(View.GONE);
        llFile.setVisibility(View.GONE);
        ivPlay.setVisibility(View.GONE);
        
        if (msg.getMessageType() != null && msg.getMessageType() == 5) {
            if (msg.getFileType() != null && msg.getFileType() == 2) {
                // FILE_UPLOAD
                llFile.setVisibility(View.VISIBLE);
                tvFileName.setText(msg.getFileName() != null ? msg.getFileName() : "未知文件");
                tvFileSize.setText(formatFileSize(msg.getFileSize()));
            } else {
                // MEDIA_CHAT (Image/Video)
                flMedia.setVisibility(View.VISIBLE);
                if (msg.getFileType() != null && msg.getFileType() == 1) {
                    ivPlay.setVisibility(View.VISIBLE); // Video
                }
                
                // 加载图片或视频封面
                String mediaUrl = "";
                if (msg.getMessageContent() != null && msg.getMessageContent().startsWith("/")) {
                    // 优先使用本地路径
                    mediaUrl = msg.getMessageContent();
                } else if (msg.getMessageId() != null && msg.getStatus() != null && msg.getStatus() == 1) {
                    mediaUrl = "http://82.157.200.53:5050/api/chat/downloadFile?fileId=" + msg.getMessageId() + "&showCover=true";
                }
                
                if (!mediaUrl.isEmpty()) {
                    Object loadModel = mediaUrl;
                    if (mediaUrl.startsWith("http") && token != null && !token.isEmpty()) {
                        loadModel = new GlideUrl(mediaUrl, new LazyHeaders.Builder().addHeader("token", token).build());
                    }
                    Glide.with(context)
                        .load(loadModel)
                        .placeholder(android.R.color.darker_gray)
                        .into(ivMedia);
                }
                
                String finalMediaUrl = mediaUrl;
                flMedia.setOnClickListener(v -> {
                    if (finalMediaUrl.isEmpty()) return;
                    Intent intent = new Intent(context, MediaPreviewActivity.class);
                    if (finalMediaUrl.startsWith("/")) {
                        intent.putExtra("mediaUrl", finalMediaUrl);
                    } else {
                        intent.putExtra("mediaUrl", finalMediaUrl.replace("&showCover=true", "&showCover=false"));
                    }
                    intent.putExtra("fileName", msg.getFileName());
                    
                    if (msg.getFileType() != null && msg.getFileType() == 1) {
                        intent.putExtra("mediaType", "video");
                    } else {
                        intent.putExtra("mediaType", "image");
                    }
                    context.startActivity(intent);
                });
            }
            
        } else if (msg.getMessageType() != null && msg.getMessageType() == 6) {
            // FILE_UPLOAD
            llFile.setVisibility(View.VISIBLE);
            tvFileName.setText(msg.getFileName() != null ? msg.getFileName() : "未知文件");
            tvFileSize.setText(formatFileSize(msg.getFileSize()));
            
            llFile.setOnClickListener(v -> {
                String fileUrl = "";
                Uri uri = null;
                if (msg.getMessageContent() != null && msg.getMessageContent().startsWith("/")) {
                    fileUrl = msg.getMessageContent();
                    uri = FileProvider.getUriForFile(context, context.getPackageName() + ".fileprovider", new File(fileUrl));
                } else if (msg.getMessageId() != null && msg.getStatus() != null && msg.getStatus() == 1) {
                    fileUrl = "http://82.157.200.53:5050/api/chat/downloadFile?fileId=" + msg.getMessageId() + "&showCover=false";
                    uri = Uri.parse(fileUrl);
                }
                if (uri != null) {
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setDataAndType(uri, "*/*");
                    intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    context.startActivity(Intent.createChooser(intent, "选择应用打开文件"));
                }
            });
            
            llFile.setOnLongClickListener(v -> {
                String fileUrl = "";
                if (msg.getMessageContent() != null && msg.getMessageContent().startsWith("/")) {
                    android.widget.Toast.makeText(context, "文件已在本地: " + msg.getMessageContent(), android.widget.Toast.LENGTH_SHORT).show();
                    return true;
                } else if (msg.getMessageId() != null && msg.getStatus() != null && msg.getStatus() == 1) {
                    fileUrl = "http://82.157.200.53:5050/api/chat/downloadFile?fileId=" + msg.getMessageId() + "&showCover=false";
                }
                
                if (!fileUrl.isEmpty()) {
                    android.app.DownloadManager.Request request = new android.app.DownloadManager.Request(Uri.parse(fileUrl));
                    if (token != null && !token.isEmpty()) {
                        request.addRequestHeader("token", token);
                    }
                    request.setNotificationVisibility(android.app.DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
                    String dlName = msg.getFileName() != null ? msg.getFileName() : "downloaded_file";
                    request.setDestinationInExternalPublicDir(android.os.Environment.DIRECTORY_DOWNLOADS, dlName);
                    request.setTitle("下载文件");
                    
                    android.app.DownloadManager dm = (android.app.DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
                    if (dm != null) {
                        dm.enqueue(request);
                        android.widget.Toast.makeText(context, "开始下载...", android.widget.Toast.LENGTH_SHORT).show();
                    }
                }
                return true;
            });
        } else {
            // CHAT
            tvMessage.setVisibility(View.VISIBLE);
            tvMessage.setText(msg.getMessageContent());
        }
    }
    
    private String formatFileSize(Long size) {
        if (size == null) return "0 B";
        if (size < 1024) return size + " B";
        int z = (63 - Long.numberOfLeadingZeros(size)) / 10;
        return String.format("%.1f %sB", (double)size / (1L << (z*10)), " KMGTPE".charAt(z));
    }

    static class MeViewHolder extends RecyclerView.ViewHolder {
        TextView tvMessage;
        View flMedia, llFile;
        ImageView ivMedia, ivPlay;
        TextView tvFileName, tvFileSize;

        MeViewHolder(View itemView) {
            super(itemView);
            tvMessage = itemView.findViewById(R.id.tvMessage);
            flMedia = itemView.findViewById(R.id.flMedia);
            llFile = itemView.findViewById(R.id.llFile);
            ivMedia = itemView.findViewById(R.id.ivMedia);
            ivPlay = itemView.findViewById(R.id.ivPlay);
            tvFileName = itemView.findViewById(R.id.tvFileName);
            tvFileSize = itemView.findViewById(R.id.tvFileSize);
        }
    }

    static class OtherViewHolder extends RecyclerView.ViewHolder {
        TextView tvMessage;
        ImageView ivAvatar;
        View flMedia, llFile;
        ImageView ivMedia, ivPlay;
        TextView tvFileName, tvFileSize;

        OtherViewHolder(View itemView) {
            super(itemView);
            tvMessage = itemView.findViewById(R.id.tvMessage);
            ivAvatar = itemView.findViewById(R.id.ivAvatar);
            flMedia = itemView.findViewById(R.id.flMedia);
            llFile = itemView.findViewById(R.id.llFile);
            ivMedia = itemView.findViewById(R.id.ivMedia);
            ivPlay = itemView.findViewById(R.id.ivPlay);
            tvFileName = itemView.findViewById(R.id.tvFileName);
            tvFileSize = itemView.findViewById(R.id.tvFileSize);
        }
    }
}
