package com.example.chatapp.utils;

import android.app.DownloadManager;
import android.content.Context;
import android.net.Uri;
import android.os.Environment;
import android.widget.Toast;

import androidx.annotation.NonNull;

import java.io.File;

public class FileDownloaderUtil {

    public static final int TYPE_IMAGE = 1;
    public static final int TYPE_VIDEO = 2;
    public static final int TYPE_FILE = 3;

    public static void downloadFile(
            @NonNull Context context,
            @NonNull String url,
            @NonNull String fileName,
            int fileType,
            String token) {

        if (url.startsWith("/")) {
            Toast.makeText(context, "文件已在本地：" + url, Toast.LENGTH_SHORT).show();
            return;
        }

        DownloadManager.Request request;
        try {
            request = new DownloadManager.Request(Uri.parse(url));
        } catch (Exception e) {
            Toast.makeText(context, "无效的下载链接", Toast.LENGTH_SHORT).show();
            return;
        }

        if (token != null && !token.isEmpty()) {
            request.addRequestHeader("token", token);
        }

        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
        request.setTitle("下载文件");
        request.setDescription(fileName);

        String destinationDir;
        if (fileType == TYPE_IMAGE) {
            destinationDir = Environment.DIRECTORY_PICTURES;
        } else if (fileType == TYPE_VIDEO) {
            destinationDir = Environment.DIRECTORY_MOVIES;
        } else {
            destinationDir = Environment.DIRECTORY_DOWNLOADS;
        }

        // Handle scoped storage gracefully by saving to public external directories
        try {
            request.setDestinationInExternalPublicDir(destinationDir, fileName);
        } catch (Exception e) {
            // Fallback to downloads if specific directory fails
            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName);
        }

        request.setAllowedOverMetered(true);
        request.setAllowedOverRoaming(true);

        DownloadManager dm = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
        if (dm != null) {
            try {
                dm.enqueue(request);
                Toast.makeText(context, "开始下载 " + fileName + "...", Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                Toast.makeText(context, "下载失败：" + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(context, "无法获取下载服务", Toast.LENGTH_SHORT).show();
        }
    }
}
