package com.example.chatapp;

public class AppConfig {
    /**
     * 服务器真实 IP
     * 提示：上传 GitHub 前，请修改为 127.0.0.1 或在此类上使用 .gitignore，防止服务器 IP 泄露
     */
    public static final String SERVER_IP = "82.157.200.53";

    /**
     * HTTP 接口基础地址（nginx 443 反向代理）
     */
    public static final String HTTP_BASE_URL = "https://" + SERVER_IP + "/api";

    /**
     * WebSocket 接口基础地址（nginx 443 wss 反向代理）
     */
    public static final String WS_BASE_URL = "wss://" + SERVER_IP + "/ws";

    /**
     * 文件下载基础地址
     */
    public static final String FILE_BASE_URL = HTTP_BASE_URL + "/chat/downloadFile?fileId=";
    /**
     * 朋友圈图片下载基础地址
     */
    public static final String MOMENT_IMAGE_BASE_URL = HTTP_BASE_URL + "/moment/downloadImage?imageUrl=";
}
