package com.example.chatapp.util;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.example.chatapp.R;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.progressindicator.LinearProgressIndicator;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class ChatFileDownloader {

    private static final OkHttpClient client = new OkHttpClient();

    public interface DownloadListener {
        void onSuccess(String filePath);
        void onFailed(String errorMsg);
    }

    public static void download(Context context, String url, String fileName, String token, DownloadListener listener) {
        if (!(context instanceof Activity)) {
            Toast.makeText(context, "上下文错误", Toast.LENGTH_SHORT).show();
            return;
        }

        View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_download_progress, null);
        TextView tvProgressTitle = dialogView.findViewById(R.id.tvProgressTitle);
        LinearProgressIndicator progressIndicator = dialogView.findViewById(R.id.progressIndicator);
        TextView tvProgressText = dialogView.findViewById(R.id.tvProgressText);

        tvProgressTitle.setText("正在下载 " + (fileName != null ? fileName : "文件"));
        progressIndicator.setMax(100);
        progressIndicator.setProgress(0);

        AlertDialog dialog = new MaterialAlertDialogBuilder(context)
                .setView(dialogView)
                .setCancelable(false)
                .setNegativeButton("取消", null)
                .create();

        dialog.show();

        Request.Builder requestBuilder = new Request.Builder().url(url);
        if (token != null && !token.isEmpty()) {
            requestBuilder.addHeader("token", token);
        }
        Request request = requestBuilder.build();
        Call call = client.newCall(request);

        dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setOnClickListener(v -> {
            call.cancel();
            dialog.dismiss();
            Toast.makeText(context, "下载已取消", Toast.LENGTH_SHORT).show();
        });

        Handler mainHandler = new Handler(Looper.getMainLooper());

        call.enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                if (call.isCanceled()) return;
                mainHandler.post(() -> {
                    dialog.dismiss();
                    Toast.makeText(context, "下载失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    if (listener != null) listener.onFailed(e.getMessage());
                });
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    mainHandler.post(() -> {
                        dialog.dismiss();
                        Toast.makeText(context, "下载失败，状态码: " + response.code(), Toast.LENGTH_SHORT).show();
                        if (listener != null) listener.onFailed("HTTP " + response.code());
                    });
                    return;
                }

                File dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                if (!dir.exists()) {
                    dir.mkdirs();
                }

                String finalFileName = fileName;
                if (finalFileName == null || finalFileName.isEmpty()) {
                    finalFileName = "download_" + System.currentTimeMillis();
                }
                
                File file = new File(dir, finalFileName);
                int count = 1;
                while (file.exists()) {
                    int dotIndex = finalFileName.lastIndexOf(".");
                    if (dotIndex != -1) {
                        file = new File(dir, finalFileName.substring(0, dotIndex) + "(" + count + ")" + finalFileName.substring(dotIndex));
                    } else {
                        file = new File(dir, finalFileName + "(" + count + ")");
                    }
                    count++;
                }

                try (InputStream is = response.body().byteStream();
                     FileOutputStream fos = new FileOutputStream(file)) {
                    
                    long contentLength = response.body().contentLength();
                    byte[] buffer = new byte[4096];
                    long totalRead = 0;
                    int read;
                    long lastUpdate = 0;

                    while ((read = is.read(buffer)) != -1) {
                        if (call.isCanceled()) {
                            file.delete();
                            return;
                        }
                        fos.write(buffer, 0, read);
                        totalRead += read;

                        if (contentLength > 0) {
                            int progress = (int) (totalRead * 100 / contentLength);
                            long now = System.currentTimeMillis();
                            if (now - lastUpdate > 100 || progress == 100) {
                                lastUpdate = now;
                                mainHandler.post(() -> {
                                    if (dialog.isShowing()) {
                                        progressIndicator.setProgress(progress);
                                        tvProgressText.setText(progress + "%");
                                    }
                                });
                            }
                        }
                    }
                    
                    fos.flush();
                    
                    String downloadedPath = file.getAbsolutePath();
                    File finalFile = file;
                    mainHandler.post(() -> {
                        dialog.dismiss();
                        Toast.makeText(context, "下载完成，已保存至下载目录", Toast.LENGTH_SHORT).show();
                        // 通知媒体库扫描
                        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                        mediaScanIntent.setData(Uri.fromFile(finalFile));
                        context.sendBroadcast(mediaScanIntent);
                        if (listener != null) listener.onSuccess(downloadedPath);
                    });

                } catch (IOException e) {
                    if (call.isCanceled()) return;
                    mainHandler.post(() -> {
                        dialog.dismiss();
                        Toast.makeText(context, "保存文件失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        if (listener != null) listener.onFailed(e.getMessage());
                    });
                }
            }
        });
    }
}