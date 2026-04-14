package com.example.chatapp;

public class AppConfig {
    /**
     * 服务器真实 IP
     * 提示：上传 GitHub 前，请修改为 127.0.0.1 或在此类上使用 .gitignore，防止服务器 IP 泄露
     */
    public static final String SERVER_IP = "82.157.200.53";

    /**
     * HTTP 接口基础地址
     */
    public static final String HTTP_BASE_URL = "http://" + SERVER_IP + ":5050/api";

    /**
     * WebSocket 接口基础地址
     */
    public static final String WS_BASE_URL = "ws://" + SERVER_IP + ":5051/ws";

    /**
     * 文件下载基础地址
     */
    public static final String FILE_BASE_URL = HTTP_BASE_URL + "/chat/downloadFile?fileId=";
}
