package com.example.chatapp.chat;

import android.Manifest;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.load.model.LazyHeaders;
import com.example.chatapp.R;
import com.example.chatapp.UserDao;
import com.github.chrisbanes.photoview.PhotoView;

import androidx.media3.common.MediaItem;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.source.MediaSource;
import androidx.media3.exoplayer.source.ProgressiveMediaSource;
import androidx.media3.datasource.DefaultHttpDataSource;
import androidx.media3.datasource.DefaultDataSource;
import androidx.media3.ui.PlayerView;

public class MediaPreviewActivity extends AppCompatActivity {

    private PhotoView photoView;
    private PlayerView playerView;
    private ExoPlayer player;
    private String mediaUrl;
    private String mediaType; // "image" or "video"
    private String fileName;
    private String token;
    private long downloadId = -1;

    private final BroadcastReceiver downloadReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            long id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
            if (id == downloadId) {
                DownloadManager dm = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
                DownloadManager.Query query = new DownloadManager.Query();
                query.setFilterById(id);
                try (Cursor cursor = dm.query(query)) {
                    if (cursor != null && cursor.moveToFirst()) {
                        int statusIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS);
                        int uriIndex = cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI);
                        if (statusIndex >= 0) {
                            int status = cursor.getInt(statusIndex);
                            if (status == DownloadManager.STATUS_SUCCESSFUL) {
                                Toast.makeText(MediaPreviewActivity.this, "下载完成", Toast.LENGTH_SHORT).show();
                                Log.i("MediaPreviewActivity", "Download successful: " + fileName);
                                
                                if (uriIndex >= 0) {
                                    String localUri = cursor.getString(uriIndex);
                                    if (localUri != null) {
                                        // 扫描文件以便在相册中显示
                                        Uri fileUri = Uri.parse(localUri);
                                        sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, fileUri));
                                    }
                                }
                            } else if (status == DownloadManager.STATUS_FAILED) {
                                int reasonIndex = cursor.getColumnIndex(DownloadManager.COLUMN_REASON);
                                int reason = reasonIndex >= 0 ? cursor.getInt(reasonIndex) : -1;
                                Toast.makeText(MediaPreviewActivity.this, "下载失败，错误码：" + reason, Toast.LENGTH_SHORT).show();
                                Log.e("MediaPreviewActivity", "Download failed, reason: " + reason);
                            }
                        }
                    }
                }
            }
        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Full screen
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        setContentView(R.layout.activity_media_preview);

        mediaUrl = getIntent().getStringExtra("mediaUrl");
        mediaType = getIntent().getStringExtra("mediaType");
        fileName = getIntent().getStringExtra("fileName");
        if (fileName == null) {
            fileName = "downloaded_media_" + System.currentTimeMillis();
            if ("image".equals(mediaType)) fileName += ".jpg";
            else if ("video".equals(mediaType)) fileName += ".mp4";
        }
        
        token = UserDao.getInstance().getUser() != null ? UserDao.getInstance().getUser().getToken() : "";

        photoView = findViewById(R.id.photoView);
        playerView = findViewById(R.id.playerView);
        findViewById(R.id.btnClose).setOnClickListener(v -> finish());
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(downloadReceiver, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE), Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(downloadReceiver, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
        }

        if ("image".equals(mediaType)) {
            photoView.setVisibility(View.VISIBLE);
            Object loadModel = mediaUrl;
            if (mediaUrl.startsWith("http") && token != null && !token.isEmpty()) {
                loadModel = new GlideUrl(mediaUrl, new LazyHeaders.Builder().addHeader("token", token).build());
            }
            Glide.with(this).load(loadModel).into(photoView);
            
            photoView.setOnLongClickListener(v -> {
                downloadMedia();
                return true;
            });
        } else if ("video".equals(mediaType)) {
            playerView.setVisibility(View.VISIBLE);
            player = new ExoPlayer.Builder(this).build();
            playerView.setPlayer(player);
            
            Uri uri;
            if (mediaUrl.startsWith("/")) {
                uri = Uri.parse("file://" + mediaUrl);
            } else {
                uri = Uri.parse(mediaUrl);
            }
            
            // Note: ExoPlayer with headers is slightly more complex.
            // For simplicity and since ExoPlayer doesn't easily support arbitrary headers in simple MediaItem,
            // we will pass the token in URL if backend supports it, or assume simple token header.
            // Let's use DefaultHttpDataSource for ExoPlayer to pass headers.
            DefaultHttpDataSource.Factory dataSourceFactory = 
                new DefaultHttpDataSource.Factory();
            
            if (token != null && !token.isEmpty()) {
                java.util.Map<String, String> headers = new java.util.HashMap<>();
                headers.put("token", token);
                dataSourceFactory.setDefaultRequestProperties(headers);
            }
                    
            MediaSource mediaSource;
            if (mediaUrl.startsWith("http")) {
                 mediaSource = new ProgressiveMediaSource.Factory(dataSourceFactory)
                    .createMediaSource(MediaItem.fromUri(uri));
            } else {
                 mediaSource = new ProgressiveMediaSource.Factory(new DefaultDataSource.Factory(this))
                    .createMediaSource(MediaItem.fromUri(uri));
            }
            
            player.setMediaSource(mediaSource);
            player.prepare();
            player.play();
            
            playerView.setOnLongClickListener(v -> {
                downloadMedia();
                return true;
            });
        }
    }

    private void downloadMedia() {
        if (mediaUrl.startsWith("/")) {
            Toast.makeText(this, "文件已在本地：" + mediaUrl, Toast.LENGTH_SHORT).show();
            return;
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
                return;
            }
        }

        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(mediaUrl));
        request.addRequestHeader("token", token);
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName);
        request.setTitle("下载媒体文件");

        DownloadManager dm = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
        if (dm != null) {
            Log.i("MediaPreviewActivity", "Starting download: " + mediaUrl);
            downloadId = dm.enqueue(request);
            Toast.makeText(this, "开始下载...", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1 && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            downloadMedia();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (player != null) {
            player.pause();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        try {
            unregisterReceiver(downloadReceiver);
        } catch (IllegalArgumentException e) {
            Log.e("MediaPreviewActivity", "Receiver not registered", e);
        }
        if (player != null) {
            player.release();
            player = null;
        }
    }
}