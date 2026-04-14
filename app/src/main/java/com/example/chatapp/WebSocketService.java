package com.example.chatapp;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkRequest;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.example.chatapp.R;
import com.example.chatapp.data.MessageSendDto;
import com.example.chatapp.data.WsInitData;
import com.example.chatapp.database.AppDatabase;
import com.example.chatapp.database.ChatMessageEntity;
import com.example.chatapp.database.ChatSessionEntity;
import com.example.chatapp.chat.ChatActivity;
import com.example.chatapp.util.AppExecutors;
import com.example.chatapp.util.ChatSessionIdUtils;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class WebSocketService extends Service {
    private static final String TAG = "WebSocketService";
    private static final String CHANNEL_ID = "WebSocketChannel";
    private static final int NOTIFICATION_ID = 1001;

    // 广播相关常量
    public static final String ACTION_WS_MESSAGE = "com.example.chatapp.WS_MESSAGE";
    public static final String ACTION_MANUAL_STOP = "com.example.chatapp.ACTION_MANUAL_STOP";
    public static final String ACTION_WS_STATUS = "com.example.chatapp.WS_STATUS"; // 新增：状态广播（连接/断开/重连）
    public static final String ACTION_WS_RECONNECTED = "com.example.chatapp.WS_RECONNECTED"; // 新增：重连成功广播
    public static final String EXTRA_MESSAGE = "message_content";
    public static final String EXTRA_STATUS = "status_content"; // 状态内容（如“连接成功”“重连中”）
    public static final String EXTRA_USER_ID = "extra_user_id";
    public static final String EXTRA_TOKEN = "extra_token"; // 新增：传递 Token

    // WebSocket 配置
    private String currentUserId=null;
    private String currentToken = null;
    private WebSocketManager webSocketManager;
    private Handler mainHandler; // 主线程 Handler，用于延迟重连
    private final Gson gson = new Gson();

    // 重连控制参数
    private int reconnectDelay = 3000; // 初始重连延迟（3秒）
    private static final int MAX_RECONNECT_DELAY = 30000; // 最大重连延迟（30秒）
    private boolean isManualStop = false; // 是否手动停止服务（避免手动停止后重连）

    private Runnable reconnectRunnable = new Runnable() {
        @Override
        public void run() {
            // 重连前再次校验用户信息（防止UserDao数据变化）
            try {
                User user = UserDao.getInstance().getUser();
                if (user != null) {
                    currentUserId = user.getUserId();
                    currentToken = user.getToken();
                }
            } catch (Exception e) {
                Log.e(TAG, "重连前获取用户信息失败", e);
            }

            // 若用户信息仍有效，启动服务触发重连（空Intent即可，onStartCommand会补充信息）
            if (currentUserId != null && currentToken != null && !currentToken.isEmpty()) {
                Intent restartIntent = new Intent(WebSocketService.this, WebSocketService.class);
                startService(restartIntent);
                // 重连延迟指数增长（最多30秒）
                reconnectDelay = Math.min(reconnectDelay * 2, MAX_RECONNECT_DELAY);
            } else {
                Log.e(TAG, "用户信息失效，停止重连");
                stopSelf();
            }
        }
    };

    private ConnectivityManager connectivityManager;
    private ConnectivityManager.NetworkCallback networkCallback;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "WebSocketService 创建");
        // 初始化：WebSocketManager、主线程Handler、前台服务
        webSocketManager = WebSocketManager.getInstance();
        mainHandler = new Handler(Looper.getMainLooper());
        createNotificationChannel();
        createMessageNotificationChannel(); // 新增：初始化消息通知 Channel
        startForeground(NOTIFICATION_ID, createNotification()); // 启动为前台服务，避免被系统回收

        registerNetworkCallback();
    }

    private void registerNetworkCallback() {
        connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager == null) return;

        networkCallback = new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(Network network) {
                super.onAvailable(network);
                Log.d(TAG, "网络恢复可用");
                // 网络恢复时，若未连接且非手动停止，则立即重连
                if (!isManualStop && !webSocketManager.isConnected()) {
                    Log.d(TAG, "触发网络恢复立即重连");
                    reconnectDelay = 0; // 【优化】重置延迟为0，实现光速重连
                    mainHandler.removeCallbacks(reconnectRunnable); // P0-1: 不要清空全部任务
                    // P1: 短延迟 500ms 等待路由真正可达，减少无效重连风暴
                    mainHandler.postDelayed(() -> scheduleReconnect(), 500);
                }
            }

            @Override
            public void onLost(Network network) {
                super.onLost(network);
                Log.d(TAG, "网络断开");
                // 网络断开时，WebSocket 可能无法立即检测到，手动断开它以触发重连流程
                if (webSocketManager.isConnected()) {
                    webSocketManager.close();
                }
            }
        };

        NetworkRequest request = new NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build();
        connectivityManager.registerNetworkCallback(request, networkCallback);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "WebSocketService 启动（startId: " + startId + "）");
        
        if (intent != null && ACTION_MANUAL_STOP.equals(intent.getAction())) {
            isManualStop = true;
            mainHandler.removeCallbacks(reconnectRunnable);
            webSocketManager.close();
            stopSelf();
            return START_NOT_STICKY;
        }

        isManualStop = false; // 服务启动时，重置手动停止标记

        // 1. 获取用户ID和Token（优先从Intent拿，其次从UserDao拿）
        if (intent != null) {
            currentUserId = intent.getStringExtra(EXTRA_USER_ID);
            currentToken = intent.getStringExtra(EXTRA_TOKEN);
        }
        // 若Intent未携带或无效，从UserDao补充
        if (currentUserId == null || currentToken == null || currentToken.isEmpty()) {
            try {
                User user = UserDao.getInstance().getUser();
                if (user != null) {
                    currentUserId = user.getUserId();
                    currentToken = user.getToken();
                }
            } catch (Exception e) {
                Log.e(TAG, "从UserDao获取用户信息失败", e);
            }
        }

        // 2. 校验用户信息，启动连接
        Log.d(TAG, "当前用户ID: " + currentUserId + "，Token: " + (currentToken != null ? currentToken.substring(0, 10) + "..." : "null"));
        if (currentUserId != null && currentToken != null && !currentToken.isEmpty()) {
            // 只有未连接时才发起连接（避免重复连接）
            if (!webSocketManager.isConnected()) {
                webSocketManager.connect(String.valueOf(currentUserId), currentToken, createServiceListener());
            }
            return START_STICKY; // 服务被杀死后，系统会用空Intent重启，触发重连
        } else {
            Log.e(TAG, "用户ID或Token无效，停止服务");
            stopSelf();
            return START_NOT_STICKY;
        }
    }

    // 创建WebSocket监听器（处理连接状态和消息）
    private WebSocketManager.WebSocketListener createServiceListener() {
        return new WebSocketManager.WebSocketListener() {
            @Override
            public void onOpen() {
                // 切换到主线程发送广播
                mainHandler.post(() -> {
                    Log.d(TAG, "WebSocket 连接成功");
                    // 连接成功：重置重连延迟，发送连接成功广播
                    reconnectDelay = 3000;
                    sendStatusBroadcast("连接成功");

                    // P1-2: 离线补偿双保险机制（校验离线期间是否有消息遗漏）
                    checkOfflineMessageGap();

                    // 解决消息丢失：发送重连成功广播，通知 UI 拉取离线消息
                    Intent reconnectedIntent = new Intent(ACTION_WS_RECONNECTED);
                    LocalBroadcastManager.getInstance(WebSocketService.this).sendBroadcast(reconnectedIntent);
                });
            }

            @Override
            public void onMessage(String message) {
                persistIncomingPayload(message);
                // 切换到主线程发送消息广播，避免 UI 崩溃
                mainHandler.post(() -> {
                    Log.d(TAG, "收到消息: " + message);
                    try {
                        MessageSendDto<?> base = gson.fromJson(message, MessageSendDto.class);
                        if (base != null && base.getMessageType() != null) {
                            if (base.getMessageType() == 7) {
                                isManualStop = true;
                                webSocketManager.close();
                                stopSelf();
                                MyApplication.getInstance().handleForceOffline();
                                return;
                            } else if (base.getMessageType() == 2) {
                                // 拦截 CHAT(2) 类型消息，触发通知
                                String senderName = base.getSendUserNickName();
                                String content = base.getMessageContent();
                                String contactId = base.getContactId();
                                // 处理发送给自己时的视角转换
                                if (contactId != null && contactId.equals(currentUserId)) {
                                    contactId = base.getSendUserId();
                                }
                                if (senderName != null && content != null && contactId != null) {
                                    showNewMessageNotification(senderName, content, contactId);
                                }
                            }
                        }
                    } catch (Exception ignore) {
                    }
                    // 转发消息给Fragment/Activity
                    Intent msgIntent = new Intent(ACTION_WS_MESSAGE);
                    msgIntent.putExtra(EXTRA_MESSAGE, message);
                    LocalBroadcastManager.getInstance(WebSocketService.this).sendBroadcast(msgIntent);
                });
            }
 
            @Override
            public void onClose(int code, String reason) {
                mainHandler.post(() -> {
                    Log.w(TAG, "连接关闭 | 代码: " + code + " | 原因: " + reason);
                    sendStatusBroadcast("连接关闭：" + reason);

                    // P0-2: 去掉 code != 1000 限制。仅根据 isManualStop 判定是否重连。
                    // 解决本地主动 close（此时code可能是1000）被误判为正常退出不重连的问题。
                    if (!isManualStop) {
                        scheduleReconnect();
                    }
                });
            }

            @Override
            public void onError(Exception ex) {
                mainHandler.post(() -> {
                    Log.e(TAG, "连接出错", ex);
                    sendStatusBroadcast("连接错误：" + ex.getMessage());
                    // 错误发生后，触发重连（排除手动停止场景）
                    if (!isManualStop) {
                        scheduleReconnect();
                    }
                });
            }
        };
    }

    private void checkOfflineMessageGap() {
        // P1-2: 离线补偿双保险
        // 虽然 INIT 包含了离线消息，但可能存在极端边界导致服务器未即时感知离线。
        // 此处应通过 HTTP 接口传入客户端最后一条消息的时间戳/ID，拉取增量数据进行对比补偿。
        // 目前暂作日志记录，后续若后端补充增量接口可在此处调用。
        Log.i(TAG, "执行离线消息缺口校验（双保险补偿机制）");
        // TODO: 调用增量同步 API
    }

    private void persistIncomingPayload(String payload) {
        if (payload == null || payload.isEmpty()) return;

        AppExecutors.io().execute(() -> {
            try {
                MessageSendDto<?> base = gson.fromJson(payload, MessageSendDto.class);
                if (base == null || base.getMessageType() == null) return;
                int mt = base.getMessageType();
                if (mt == 0) {
                    Type t = new TypeToken<MessageSendDto<WsInitData>>() {
                    }.getType();
                    MessageSendDto<WsInitData> initMsg = gson.fromJson(payload, t);
                    WsInitData data = initMsg != null ? initMsg.getExtendData() : null;
                    if (data != null) {
                        persistInitData(data);
                    }
                    return;
                }
                if (mt == 2 || mt == 5 || mt == 6) {
                    persistChatMessage(base);
                }
                if (mt == 8) {
                    // 解散群聊
                    String contactId = base.getContactId();
                    if (contactId != null) {
                        String sessionId = ChatSessionIdUtils.getChatSessionId4Group(contactId);
                        AppDatabase db = AppDatabase.getInstance(WebSocketService.this);
                        db.chatSessionDao().deleteSessionById(sessionId);
                        db.chatMessageDao().deleteMessagesBySessionId(sessionId);
                    }
                }
                if (mt == 11) {
                    // 退出群聊：如果是自己退出的（可能是多端同步，或者是其他人退群）
                    // 考虑到别人退群不应该删除我的会话，只有当 sendUserId == currentUserId 时我才删除我的会话
                    String contactId = base.getContactId();
                    if (contactId != null && currentUserId != null && currentUserId.equals(base.getSendUserId())) {
                        String sessionId = ChatSessionIdUtils.getChatSessionId4Group(contactId);
                        AppDatabase db = AppDatabase.getInstance(WebSocketService.this);
                        db.chatSessionDao().deleteSessionById(sessionId);
                        db.chatMessageDao().deleteMessagesBySessionId(sessionId);
                    }
                }
                if (mt == 12) {
                    // 踢出群聊：被踢出的用户如果是自己，需要删除会话
                    // 这里我们为了稳妥，也可以在接收到此消息时请求联系人刷新，如果群聊不在了，就在UI里表现出来
                    // 实际上后端发来的 remove_group 消息可能把被踢人的ID放在 extendData 里
                    String contactId = base.getContactId();
                    String extendData = base.getExtendData() != null ? String.valueOf(base.getExtendData()) : "";
                    if (contactId != null && currentUserId != null && extendData.contains(currentUserId)) {
                        String sessionId = ChatSessionIdUtils.getChatSessionId4Group(contactId);
                        AppDatabase db = AppDatabase.getInstance(WebSocketService.this);
                        db.chatSessionDao().deleteSessionById(sessionId);
                        db.chatMessageDao().deleteMessagesBySessionId(sessionId);
                    }
                }
            } catch (Exception ignored) {
            }
        });
    }

    private void persistInitData(WsInitData data) {
        AppDatabase db = AppDatabase.getInstance(this);

        List<WsInitData.ChatSessionUser> sessionList = data.getChatSessionList();
        if (sessionList != null && !sessionList.isEmpty()) {
            List<ChatSessionEntity> sessionEntities = new ArrayList<>();
            for (WsInitData.ChatSessionUser session : sessionList) {
                ChatSessionEntity entity = new ChatSessionEntity();
                entity.setSessionId(session.getSessionId());
                entity.setContactId(session.getContactId());
                Integer contactType = ChatSessionIdUtils.getContactTypeById(session.getContactId());
                if (contactType != null) {
                    entity.setContactType(contactType);
                }
                entity.setContactName(session.getContactName());
                entity.setLastMessage(session.getLastMessage());
                entity.setLastReceiveTime(session.getLastReceiveTime());
                entity.setMemberCount(session.getMemberCount() != null ? session.getMemberCount() : 0);
                sessionEntities.add(entity);
            }
            db.chatSessionDao().insertOrReplaceList(sessionEntities);
        }

        List<WsInitData.ChatMessageDto> messageList = data.getChatMessageList();
        if (messageList != null && !messageList.isEmpty()) {
            List<ChatMessageEntity> messageEntities = new ArrayList<>();
            for (WsInitData.ChatMessageDto msg : messageList) {
                Long messageId = msg.getMessageId();
                if (messageId != null && db.chatMessageDao().getMessageByRemoteId(messageId) != null) {
                    continue;
                }
                ChatMessageEntity entity = new ChatMessageEntity();
                if (messageId != null) {
                    entity.setLocalId("remote_" + messageId);
                }
                entity.setMessageId(messageId);
                entity.setClientOrderTime(0L);
                entity.setSessionId(msg.getSessionId());
                entity.setMessageType(msg.getMessageType());
                entity.setMessageContent(msg.getMessageContent());
                entity.setSendUserId(msg.getSendUserId());
                entity.setSendUserNickName(msg.getSendUserNickName());
                entity.setSendTime(msg.getSendTime());
                entity.setContactId(msg.getContactId());
                entity.setContactType(msg.getContactType());
                entity.setFileSize(msg.getFileSize());
                entity.setFileName(msg.getFileName());
                entity.setFileType(msg.getFileType());
                entity.setStatus(msg.getStatus() != null ? msg.getStatus() : 1);
                messageEntities.add(entity);
            }
            if (!messageEntities.isEmpty()) {
                db.chatMessageDao().insertOrReplaceList(messageEntities);
            }
        }
    }

    private void persistChatMessage(MessageSendDto<?> base) {
        AppDatabase db = AppDatabase.getInstance(this);

        if (base.getMessageId() != null) {
            ChatMessageEntity existed = db.chatMessageDao().getMessageByRemoteId(base.getMessageId());
            if (existed != null) {
                boolean changed = false;
                if (base.getStatus() != null && !base.getStatus().equals(existed.getStatus())) {
                    existed.setStatus(base.getStatus());
                    changed = true;
                }
                if (base.getFileSize() != null && base.getFileSize() > 0 && (existed.getFileSize() == null || existed.getFileSize() == 0)) {
                    existed.setFileSize(base.getFileSize());
                    existed.setFileName(base.getFileName());
                    existed.setFileType(base.getFileType());
                    changed = true;
                }
                if (changed) {
                    db.chatMessageDao().insertOrReplace(existed);
                }
                return;
            }
        }

        String myUserId = currentUserId;
        String contactId = base.getContactId();
        String sessionId = base.getSessionId();
        if (sessionId == null || sessionId.isEmpty()) {
            Integer ct = ChatSessionIdUtils.getContactTypeById(contactId);
            if (ct != null && ct == 1) {
                sessionId = ChatSessionIdUtils.getChatSessionId4Group(contactId);
            } else {
                sessionId = ChatSessionIdUtils.getChatSessionId4User(myUserId, contactId);
            }
        }

        ChatSessionEntity session = db.chatSessionDao().getSessionById(sessionId);
        if (session == null) {
            session = new ChatSessionEntity();
            session.setSessionId(sessionId);
            session.setContactId(contactId);
            session.setContactName(base.getContactName() != null ? base.getContactName() : contactId);
            Integer ct = base.getContactType();
            if (ct == null) {
                ct = ChatSessionIdUtils.getContactTypeById(contactId);
            }
            session.setContactType(ct);
            db.chatSessionDao().insertOrReplace(session);
        }

        ChatMessageEntity entity = new ChatMessageEntity();
        if (base.getMessageId() != null) {
            entity.setLocalId("remote_" + base.getMessageId());
        }
        entity.setMessageId(base.getMessageId());
        entity.setClientOrderTime(0L);
        entity.setSessionId(sessionId);
        entity.setMessageType(base.getMessageType());
        entity.setMessageContent(base.getMessageContent());
        entity.setSendUserId(base.getSendUserId());
        entity.setSendUserNickName(base.getSendUserNickName());
        entity.setSendTime(base.getSendTime());
        entity.setContactId(base.getContactId());
        entity.setContactType(base.getContactType());
        entity.setFileSize(base.getFileSize());
        entity.setFileName(base.getFileName());
        entity.setFileType(base.getFileType());
        entity.setStatus(base.getStatus() != null ? base.getStatus() : 1);
        db.chatMessageDao().insertOrReplace(entity);

        String lastMsg = base.getLastMessage() != null ? base.getLastMessage() : base.getMessageContent();
        Long time = base.getSendTime() != null ? base.getSendTime() : System.currentTimeMillis();
        String active = ChatActivity.getActiveSessionId();
        if (active != null && active.equals(sessionId)) {
            db.chatSessionDao().updateLastMsg(sessionId, lastMsg, time);
        } else {
            db.chatSessionDao().updateUnreadAndLastMsg(sessionId, lastMsg, time);
        }
    }

    // 调度重连（指数退避策略）
    private void scheduleReconnect() {
        if (isManualStop) return; // 双重保险
        
        // P0-1: 防抖：仅移除之前的重连任务，避免清空其他主线程正常排队的 UI 操作
        mainHandler.removeCallbacks(reconnectRunnable);

        Log.d(TAG, "将在 " + reconnectDelay / 1000 + " 秒后尝试重连");
        sendStatusBroadcast("将在 " + reconnectDelay / 1000 + " 秒后重连...");

        // 用主线程Handler延迟执行重连（避免子线程直接操作服务）
        mainHandler.postDelayed(reconnectRunnable, reconnectDelay);
    }

    // 发送状态广播（连接/断开/重连等状态，供UI显示）
    private void sendStatusBroadcast(String status) {
        Intent statusIntent = new Intent(ACTION_WS_STATUS);
        statusIntent.putExtra(EXTRA_STATUS, status);
        LocalBroadcastManager.getInstance(this).sendBroadcast(statusIntent);
    }

    // 手动停止服务（如用户退出登录时调用）
    public void stopServiceManually() {
        isManualStop = true; // 标记为手动停止，避免重连
        webSocketManager.close(); // 关闭WebSocket连接
        stopSelf(); // 停止服务
    }

    // ---------------- 前台服务相关 ----------------
    private Notification createNotification() {
        Notification.Builder builder = new Notification.Builder(this, CHANNEL_ID)
                .setContentTitle("聊天服务运行中")
                .setContentText("保持在线状态")
                .setSmallIcon(R.mipmap.ic_launcher) // 替换为你的应用图标
                .setPriority(Notification.PRIORITY_LOW);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            return builder.build();
        } else {
            builder.setDefaults(Notification.DEFAULT_SOUND);
            return builder.getNotification();
        }
    }

    // 新增：创建消息通知 Channel
    private static final String MESSAGE_CHANNEL_ID = "MessageChannel";
    private void createMessageNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    MESSAGE_CHANNEL_ID,
                    "新消息通知",
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("用于显示聊天新消息");
            channel.enableVibration(true);
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    // 新增：发送新消息通知
    private void showNewMessageNotification(String senderName, String messageContent, String contactId) {
        Intent intent = new Intent(this, com.example.chatapp.chat.ChatActivity.class);
        intent.putExtra("contactId", contactId);
        intent.putExtra("contactName", senderName);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        int pendingFlags = android.app.PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            pendingFlags |= android.app.PendingIntent.FLAG_IMMUTABLE;
        }
        android.app.PendingIntent pendingIntent = android.app.PendingIntent.getActivity(
                this, 
                contactId.hashCode(), 
                intent, 
                pendingFlags
        );

        Notification.Builder builder = new Notification.Builder(this, MESSAGE_CHANNEL_ID)
                .setContentTitle(senderName)
                .setContentText(messageContent)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            builder.setPriority(Notification.PRIORITY_HIGH);
            builder.setDefaults(Notification.DEFAULT_ALL);
        }

        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.notify((int) System.currentTimeMillis(), builder.build());
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "WebSocket 服务",
                    NotificationManager.IMPORTANCE_LOW
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "WebSocketService 销毁");
        isManualStop = true; // 极其重要：防止异步的 onClose 再次拉起服务
        if (connectivityManager != null && networkCallback != null) {
            connectivityManager.unregisterNetworkCallback(networkCallback);
        }
        mainHandler.removeCallbacks(reconnectRunnable); // P0-1: 先移除任务
        webSocketManager.close(); // 再关闭连接
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null; // 若不需要绑定通信，返回null；如需通信，可实现Binder
    }
}
