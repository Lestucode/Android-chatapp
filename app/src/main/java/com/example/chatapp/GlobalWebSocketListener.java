//package com.example.chatapp;
//
//import android.content.Context;
//import android.content.Intent;
//import android.util.Log;
//
//import androidx.annotation.Nullable;
//import androidx.localbroadcastmanager.content.LocalBroadcastManager;
//
//public abstract class GlobalWebSocketListener implements WebSocketManager.WebSocketListener {
//    private static final String TAG = "GlobalListener";
//    private final Context context;
//
//    // 广播 Action 常量
//    public static final String ACTION_WS_OPEN = "com.example.chatapp.WS_OPEN";
//    public static final String ACTION_WS_MESSAGE = "com.example.chatapp.WS_MESSAGE";
//    public static final String ACTION_WS_CLOSE = "com.example.chatapp.WS_CLOSE";
//    public static final String ACTION_WS_ERROR = "com.example.chatapp.WS_ERROR";
//
//    public GlobalWebSocketListener(Context context) {
//        this.context = context.getApplicationContext(); // 防止内存泄漏
//    }
//
//    @Override
//    public void onOpen() {
//        Log.d(TAG, "全局 WebSocket 连接已打开");
//        sendBroadcast(ACTION_WS_OPEN, null);
//    }
//
//    @Override
//    public void onMessage(String message) {
//        Log.d(TAG, "全局收到消息: " + message);
//        sendBroadcast(ACTION_WS_MESSAGE, message);
//    }
//
//    @Override
//    public void onClose(int code, String reason) {
//        Log.d(TAG, "全局 WebSocket 连接关闭: " + reason);
//        sendBroadcast(ACTION_WS_CLOSE, reason);
//    }
//
//    @Override
//    public void onError(Exception ex) {
//        Log.e(TAG, "全局 WebSocket 出错: ", ex);
//        sendBroadcast(ACTION_WS_ERROR, ex.getMessage());
//    }
//
//    /**
//     * 工具方法：通过 LocalBroadcastManager 向应用内广播 WebSocket 状态
//     */
//    private void sendBroadcast(String action, @Nullable String data) {
//        Intent intent = new Intent(action);
//        if (data != null) {
//            intent.putExtra("data", data);
//        }
//        LocalBroadcastManager.getInstance(context).sendBroadcast(intent);
//    }
//}
