package com.example.chatapp;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.net.URISyntaxException;

public class WebSocketManager {
    private static final String TAG = "WebSocketManager";
    private static WebSocketManager instance;
    private WebSocketClient client;

    // 【新增】volatile 确保跨线程的可见性
    private volatile boolean isConnecting = false; // P0-3: 增加连接中状态防并发

    private WebSocketListener listener;

    // 心跳机制
    private static final long HEARTBEAT_INTERVAL = 25000; // 25秒 (小于后端60秒限制)
    
    private Handler heartbeatHandler = new Handler(Looper.getMainLooper());
    private Runnable heartbeatRunnable = new Runnable() {
        @Override
        public void run() {
            if (isConnected()) {
                Log.d(TAG, "发送心跳包");
                send("heart"); // 与后端保持一致
                heartbeatHandler.postDelayed(this, HEARTBEAT_INTERVAL);
            }
        }
    };

    private void startHeartbeat() {
        stopHeartbeat();
        heartbeatHandler.postDelayed(heartbeatRunnable, HEARTBEAT_INTERVAL);
    }

    private void stopHeartbeat() {
        heartbeatHandler.removeCallbacks(heartbeatRunnable);
    }

    private WebSocketManager() {}

    public static WebSocketManager getInstance() {
        if (instance == null) {
            instance = new WebSocketManager();
        }
        return instance;
    }

    // 【新增方法】供 WebSocketService 调用，检查连接状态
    public boolean isConnected() {
        // P0-3: 改为真实 socket 状态
        return client != null && client.isOpen();
    }

    public void connect(String userId, String token, WebSocketListener listener) {
        this.listener = listener;

        try {
            // P0-3: 增加 isConnecting 防止并发
            if (isConnected() || isConnecting) {
                Log.d(TAG, "WebSocket 已经连接或正在连接中");
                return;
            }
            isConnecting = true;

            // 确保旧的客户端被销毁，防止资源泄漏
            if (client != null) {
                client.close();
                client = null;
            }

            // 构建WebSocket连接地址，包含token
            String wsUrl = "ws://82.157.200.53:5051/ws?token=" + token;
            client = new WebSocketClient(new URI(wsUrl)) {
                @Override
                public void onOpen(ServerHandshake handshakedata) {
                    if (WebSocketManager.this.client != this) return; // P0-4: 隔离回调，防止旧连接污染
                    isConnecting = false;
                    Log.d(TAG, "WebSocket 连接成功");
                    startHeartbeat(); // 启动心跳
                    if (WebSocketManager.this.listener != null) {
                        WebSocketManager.this.listener.onOpen();
                    }
                }

                @Override
                public void onMessage(String message) {
                    if (WebSocketManager.this.client != this) return; // P0-4: 隔离回调
                    Log.d(TAG, "收到消息: " + message);
                    if (WebSocketManager.this.listener != null) {
                        WebSocketManager.this.listener.onMessage(message);
                    }
                }

                @Override
                public void onClose(int code, String reason, boolean remote) {
                    if (WebSocketManager.this.client != this) return; // P0-4: 隔离回调
                    isConnecting = false;
                    stopHeartbeat(); // 停止心跳
                    Log.d(TAG, "连接关闭: " + reason);
                    if (WebSocketManager.this.listener != null) {
                        WebSocketManager.this.listener.onClose(code, reason);
                    }
                }

                @Override
                public void onError(Exception ex) {
                    if (WebSocketManager.this.client != this) return; // P0-4: 隔离回调
                    isConnecting = false;
                    stopHeartbeat(); // 停止心跳
                    Log.e(TAG, "出错: ", ex);
                    if (WebSocketManager.this.listener != null) {
                        WebSocketManager.this.listener.onError(ex);
                    }
                }
            };

            // 设置底层的 Ping/Pong 掉线检测时间为 30 秒
            client.setConnectionLostTimeout(30);
            client.connect();

        } catch (URISyntaxException e) {
            isConnecting = false;
            Log.e(TAG, "URI 格式错误: ", e);
            // 可以选择在这里调用 listener.onError(e)
        }
    }

    public void send(String msg) {
        // 优化：使用 isConnected() 检查
        if (isConnected() && client != null) {
            client.send(msg);
        } else {
            Log.w(TAG, "发送失败，WebSocket 未连接");
        }
    }

    public void close() {
        if (client != null) {
            client.close();
            client = null;
        }
        isConnecting = false;
        stopHeartbeat();
        Log.d(TAG, "WebSocket 已关闭");
    }

    // 定义一个回调接口，方便 UI 层监听
    public interface WebSocketListener {
        void onOpen();
        void onMessage(String message);
        void onClose(int code, String reason);
        void onError(Exception ex);
    }
}