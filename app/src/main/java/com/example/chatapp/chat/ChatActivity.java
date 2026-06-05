package com.example.chatapp.chat;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.SystemClock;
import android.net.Uri;
import android.database.Cursor;
import android.provider.OpenableColumns;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.recyclerview.widget.GridLayoutManager;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

import android.view.Menu;
import android.view.MenuItem;
import android.content.Intent;

import com.example.chatapp.R;
import com.example.chatapp.User;
import com.example.chatapp.UserDao;
import com.example.chatapp.chat.AddFriendRequest.HttpClient;
import com.example.chatapp.chat.AddFriendRequest.Result;
import com.example.chatapp.database.AppDatabase;
import com.example.chatapp.database.ChatMessageEntity;
import com.example.chatapp.database.ChatSessionEntity;
import com.example.chatapp.util.ChatSessionIdUtils;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import com.example.chatapp.util.AppExecutors;

public class ChatActivity extends AppCompatActivity {
    private static final String TAG = "ChatActivity";
    private static final int HISTORY_PAGE_SIZE = 20;

    private Toolbar toolbar;
    private RecyclerView rvMessages;
    private EditText etMessage;
    private View btnSend;
    
    private View btnEmoji;
    private View btnAdd;
    private View llToolsPanel;
    private View flEmojiPanel;
    private View btnToolImage;
    private View btnToolVideo;
    private View btnToolFile;
    private RecyclerView rvEmojis;
    
    private MessageAdapter adapter;
    private EmojiAdapter emojiAdapter;
    
    private ActivityResultLauncher<String> imagePickerLauncher;
    private ActivityResultLauncher<String> videoPickerLauncher;
    private ActivityResultLauncher<String> filePickerLauncher;
    
    private String contactId;
    private String contactName;
    private String myUserId;
    private String myNickName;
    private String token;
    
    private String sessionId;

    /** 线程安全的活跃会话集合（替代 volatile 单字段，支持多会话场景） */
    private static final java.util.Set<String> activeSessionIds = java.util.Collections.synchronizedSet(new java.util.HashSet<String>());

    /** 是否正在加载历史消息，防止重复请求 */
    private boolean isLoadingHistory = false;
    /** 是否还有更多历史消息可加载 */
    private boolean hasMoreHistory = true;
    /** 当前已加载的最早消息 ID（用于分页） */
    private Long oldestMessageId = null;
    
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        contactId = getIntent().getStringExtra("contactId");
        contactName = getIntent().getStringExtra("contactName");

        User user = UserDao.getInstance().getUser();
        if (user != null) {
            myUserId = user.getUserId();
            myNickName = user.getNickname();
            token = user.getToken();
        }
        
        if (myUserId != null && contactId != null) {
            Integer contactType = ChatSessionIdUtils.getContactTypeById(contactId);
            if (contactType != null && contactType == 1) {
                sessionId = ChatSessionIdUtils.getChatSessionId4Group(contactId);
            } else {
                sessionId = ChatSessionIdUtils.getChatSessionId4User(myUserId, contactId);
            }
        }

        initLaunchers();
        initView();
        
        if (sessionId != null) {
            AppDatabase.getInstance(this).chatMessageDao().getMessagesBySessionId(sessionId).observe(this, messages -> {
                if (messages != null) {
                    Log.d("DBG-MISS-QUERY", "onCreate query | sessionId=" + sessionId + " | count=" + messages.size());
                    if (messages.size() > 0) {
                        ChatMessageEntity last = messages.get(messages.size() - 1);
                        Log.d("DBG-MISS-QUERY", "last msg | localId=" + last.getLocalId() + " | messageId=" + last.getMessageId() + " | sendTime=" + last.getSendTime() + " | clientOrderTime=" + last.getClientOrderTime() + " | content=" + last.getMessageContent());
                    }
                    adapter.submitList(messages, () -> {
                        if (adapter.getItemCount() > 0) {
                            rvMessages.scrollToPosition(adapter.getItemCount() - 1);
                        }
                    });
                }
            });
            AppExecutors.io().execute(() -> AppDatabase.getInstance(this).chatSessionDao().clearUnreadCount(sessionId));
        }
    }

    private void initLaunchers() {
        imagePickerLauncher = registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
            if (uri != null) handleFileSelection(uri, 5, 0);
        });
        videoPickerLauncher = registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
            if (uri != null) handleFileSelection(uri, 5, 1);
        });
        filePickerLauncher = registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
            if (uri != null) handleFileSelection(uri, 5, 2);
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_chat, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_info) {
            Integer contactType = ChatSessionIdUtils.getContactTypeById(contactId);
            if (contactType != null && contactType == 1) {
                // Group chat info
                Intent intent = new Intent(this, GroupInfoActivity.class);
                intent.putExtra("groupId", contactId);
                startActivity(intent);
            } else {
                // Single user info
                Intent intent = new Intent(this, UserInfoActivity.class);
                intent.putExtra("userId", contactId);
                startActivity(intent);
            }
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void initView() {
        toolbar = findViewById(R.id.chat_toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(contactName != null ? contactName : contactId);
        }
        toolbar.setNavigationOnClickListener(v -> finish());
        toolbar.setOnMenuItemClickListener(item -> onOptionsItemSelected(item));

        rvMessages = findViewById(R.id.rvMessages);
        etMessage = findViewById(R.id.etMessage);
        btnSend = findViewById(R.id.btnSend);
        
        btnEmoji = findViewById(R.id.btnEmoji);
        btnAdd = findViewById(R.id.btnAdd);
        llToolsPanel = findViewById(R.id.llToolsPanel);
        flEmojiPanel = findViewById(R.id.flEmojiPanel);
        btnToolImage = findViewById(R.id.btnToolImage);
        btnToolVideo = findViewById(R.id.btnToolVideo);
        btnToolFile = findViewById(R.id.btnToolFile);
        rvEmojis = findViewById(R.id.rvEmojis);

        adapter = new MessageAdapter(this, myUserId, contactId, token);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        rvMessages.setLayoutManager(layoutManager);
        rvMessages.setAdapter(adapter);

        // 监听滚动到顶部，触发加载历史消息
        rvMessages.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                // 向上滚动（dy < 0）且到达顶部时加载更多
                if (dy < 0 && !isLoadingHistory && hasMoreHistory) {
                    int firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition();
                    if (firstVisibleItemPosition <= 2) {
                        loadMoreHistory();
                    }
                }
            }
        });

        btnSend.setOnClickListener(v -> sendMessage());
        
        btnAdd.setOnClickListener(v -> {
            flEmojiPanel.setVisibility(View.GONE);
            if (llToolsPanel.getVisibility() == View.VISIBLE) {
                llToolsPanel.setVisibility(View.GONE);
            } else {
                llToolsPanel.setVisibility(View.VISIBLE);
            }
        });
        
        btnEmoji.setOnClickListener(v -> {
            llToolsPanel.setVisibility(View.GONE);
            if (flEmojiPanel.getVisibility() == View.VISIBLE) {
                flEmojiPanel.setVisibility(View.GONE);
            } else {
                flEmojiPanel.setVisibility(View.VISIBLE);
            }
        });
        
        etMessage.setOnClickListener(v -> {
            llToolsPanel.setVisibility(View.GONE);
            flEmojiPanel.setVisibility(View.GONE);
            if (adapter.getItemCount() > 0) {
                rvMessages.postDelayed(() -> rvMessages.smoothScrollToPosition(adapter.getItemCount() - 1), 200);
            }
        });
        
        btnToolImage.setOnClickListener(v -> imagePickerLauncher.launch("image/*"));
        btnToolVideo.setOnClickListener(v -> videoPickerLauncher.launch("video/*"));
        btnToolFile.setOnClickListener(v -> filePickerLauncher.launch("*/*"));

        List<String> emojis = Arrays.asList(
            "😀", "😃", "😄", "😁", "😆", "😅", "😂", "🤣", "😊", "😇", 
            "🙂", "🙃", "😉", "😌", "😍", "🥰", "😘", "😗", "😙", "😚", 
            "😋", "😛", "😝", "😜", "🤪", "🤨", "🧐", "🤓", "😎", "🤩", 
            "🥳", "😏", "😒", "😞", "😔", "😟", "😕", "🙁", "☹️", "😣", 
            "👍", "👎", "✊", "👊", "🤛", "🤜", "👏", "🙌", "👐", "🤲"
        );
        emojiAdapter = new EmojiAdapter(emojis, emoji -> {
            etMessage.append(emoji);
        });
        rvEmojis.setLayoutManager(new GridLayoutManager(this, 7));
        rvEmojis.setAdapter(emojiAdapter);

        etMessage.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus) {
                llToolsPanel.setVisibility(View.GONE);
                flEmojiPanel.setVisibility(View.GONE);
                if (adapter.getItemCount() > 0) {
                    rvMessages.postDelayed(() -> rvMessages.smoothScrollToPosition(adapter.getItemCount() - 1), 200);
                }
            }
        });

        rvMessages.addOnLayoutChangeListener((v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> {
            if (bottom < oldBottom && adapter.getItemCount() > 0) {
                rvMessages.postDelayed(() -> rvMessages.smoothScrollToPosition(adapter.getItemCount() - 1), 100);
            }
        });
    }

    private void sendMessage() {
        String content = etMessage.getText().toString().trim();
        if (content.isEmpty()) return;
        etMessage.setText("");

        ChatMessageEntity localMsg = new ChatMessageEntity();
        localMsg.setSessionId(sessionId);
        localMsg.setMessageType(2);
        localMsg.setMessageContent(content);
        localMsg.setSendUserId(myUserId);
        localMsg.setSendUserNickName(myNickName);
        localMsg.setSendTime(0L);
        localMsg.setClientOrderTime(System.currentTimeMillis());
        localMsg.setContactId(contactId);
        Integer contactType = ChatSessionIdUtils.getContactTypeById(contactId);
        localMsg.setContactType(contactType != null ? contactType : 0);
        localMsg.setStatus(0);

        AppExecutors.io().execute(() -> {
            AppDatabase db = AppDatabase.getInstance(this);
            db.chatMessageDao().insertOrReplace(localMsg);
            
            ChatSessionEntity session = db.chatSessionDao().getSessionById(sessionId);
            if (session == null) {
                session = new ChatSessionEntity();
                session.setSessionId(sessionId);
                session.setContactId(contactId);
                session.setContactType(contactType != null ? contactType : 0);
                session.setContactName(contactName);
            }
            session.setLastMessage(content);
            session.setLastReceiveTime(System.currentTimeMillis());
            db.chatSessionDao().insertOrReplace(session);
        });

        Map<String, Object> params = new HashMap<>();
        params.put("contactId", contactId);
        params.put("messageContent", content);
        params.put("messageType", 2);

        Type type = new TypeToken<Result<Map<String, Object>>>(){}.getType();
        HttpClient.post("/chat/sendMessage", params, token, type, result -> {
            if (result != null && "success".equals(result.getStatus())) {
                Map<String, Object> data = result.getDataAs(Map.class);
                Long remoteMessageId = null;
                String remoteSessionId = null;
                Long remoteSendTime = null;
                if (data != null) {
                    try {
                        Object mid = data.get("messageId");
                        if (mid instanceof Number) {
                            remoteMessageId = ((Number) mid).longValue();
                        } else if (mid != null) {
                            remoteMessageId = Long.parseLong(String.valueOf(mid));
                        }
                    } catch (Exception ignored) {}
                    Object sid = data.get("sessionId");
                    if (sid != null) {
                        remoteSessionId = String.valueOf(sid);
                    }
                    try {
                        Object st = data.get("sendTime");
                        if (st instanceof Number) {
                            remoteSendTime = ((Number) st).longValue();
                        } else if (st != null) {
                            remoteSendTime = Long.parseLong(String.valueOf(st));
                        }
                    } catch (Exception ignored) {}
                }
                final Long remoteMessageIdFinal = remoteMessageId;
                final String remoteSessionIdFinal = remoteSessionId;
                final Long remoteSendTimeFinal = remoteSendTime;
                AppExecutors.io().execute(() -> {
                    AppDatabase db = AppDatabase.getInstance(this);
                    localMsg.setStatus(1);
                    if (remoteMessageIdFinal != null) {
                        ChatMessageEntity existed = db.chatMessageDao().getMessageByRemoteId(remoteMessageIdFinal);
                        if (existed != null && !existed.getLocalId().equals(localMsg.getLocalId())) {
                            existed.setStatus(1);
                            db.chatMessageDao().insertOrReplace(existed);
                            db.chatMessageDao().deleteByLocalId(localMsg.getLocalId());
                            return;
                        }
                        localMsg.setMessageId(remoteMessageIdFinal);
                    }
                    if (remoteSessionIdFinal != null && !remoteSessionIdFinal.isEmpty()) {
                        localMsg.setSessionId(remoteSessionIdFinal);
                    }
                    if (remoteSendTimeFinal != null && remoteSendTimeFinal > 0) {
                        localMsg.setSendTime(remoteSendTimeFinal);
                    }
                    db.chatMessageDao().insertOrReplace(localMsg);

                    if (remoteSendTimeFinal != null && remoteSendTimeFinal > 0) {
                        db.chatSessionDao().updateLastMsg(sessionId, content, remoteSendTimeFinal);
                    }
                });
            } else {
                mainHandler.post(() -> Toast.makeText(ChatActivity.this, "发送失败", Toast.LENGTH_SHORT).show());
                AppExecutors.io().execute(() -> {
                    localMsg.setStatus(2);
                    AppDatabase.getInstance(this).chatMessageDao().insertOrReplace(localMsg);
                });
            }
        });
    }

    private void handleFileSelection(Uri uri, int messageType, int fileType) {
        String fileName = getFileName(uri);
        long fileSize = getFileSize(uri);
        File localFile = uriToFile(uri, fileName);
        if (localFile != null) {
            sendFileMessage(localFile, fileName, fileSize, messageType, fileType);
            llToolsPanel.setVisibility(View.GONE);
        } else {
            Toast.makeText(this, "文件读取失败", Toast.LENGTH_SHORT).show();
        }
    }

    private String getFileName(Uri uri) {
        String result = null;
        if (uri.getScheme().equals("content")) {
            try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (index >= 0) result = cursor.getString(index);
                }
            }
        }
        if (result == null) {
            result = uri.getPath();
            int cut = result.lastIndexOf('/');
            if (cut != -1) result = result.substring(cut + 1);
        }
        return result;
    }

    private long getFileSize(Uri uri) {
        long size = 0;
        if (uri.getScheme().equals("content")) {
            try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int index = cursor.getColumnIndex(OpenableColumns.SIZE);
                    if (index >= 0) size = cursor.getLong(index);
                }
            }
        }
        return size;
    }

    private File uriToFile(Uri uri, String fileName) {
        try {
            InputStream in = getContentResolver().openInputStream(uri);
            if (in == null) return null;
            File tempFile = new File(getCacheDir(), fileName);
            FileOutputStream out = new FileOutputStream(tempFile);
            byte[] buf = new byte[1024];
            int len;
            while ((len = in.read(buf)) > 0) {
                out.write(buf, 0, len);
            }
            out.close();
            in.close();
            return tempFile;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private void sendFileMessage(File file, String fileName, long fileSize, int messageType, int fileType) {
        String localPath = file.getAbsolutePath();
        
        ChatMessageEntity localMsg = new ChatMessageEntity();
        localMsg.setSessionId(sessionId);
        localMsg.setMessageType(messageType);
        localMsg.setMessageContent(localPath);
        localMsg.setSendUserId(myUserId);
        localMsg.setSendUserNickName(myNickName);
        localMsg.setSendTime(0L);
        localMsg.setClientOrderTime(System.currentTimeMillis());
        localMsg.setContactId(contactId);
        Integer contactType = ChatSessionIdUtils.getContactTypeById(contactId);
        localMsg.setContactType(contactType != null ? contactType : 0);
        localMsg.setStatus(0);
        localMsg.setFileName(fileName);
        localMsg.setFileSize(fileSize);
        localMsg.setFileType(fileType);

        AppExecutors.io().execute(() -> {
            AppDatabase db = AppDatabase.getInstance(this);
            db.chatMessageDao().insertOrReplace(localMsg);
            
            ChatSessionEntity session = db.chatSessionDao().getSessionById(sessionId);
            if (session == null) {
                session = new ChatSessionEntity();
                session.setSessionId(sessionId);
                session.setContactId(contactId);
                session.setContactType(contactType != null ? contactType : 0);
                session.setContactName(contactName);
            }
            session.setLastMessage(messageType == 5 ? (fileType == 0 ? "[图片]" : "[视频]") : "[文件]");
            session.setLastReceiveTime(System.currentTimeMillis());
            db.chatSessionDao().insertOrReplace(session);
        });

        Map<String, Object> params = new HashMap<>();
        params.put("contactId", contactId);
        params.put("messageContent", messageType == 5 ? (fileType == 0 ? "[图片]" : "[视频]") : "[文件]"); 
        params.put("messageType", messageType);
        params.put("fileSize", fileSize);
        params.put("fileName", fileName);
        params.put("fileType", fileType);

        Type type = new TypeToken<Result<Map<String, Object>>>(){}.getType();
        HttpClient.post("/chat/sendMessage", params, token, type, result -> {
            if (result != null && "success".equals(result.getStatus())) {
                Map<String, Object> data = result.getDataAs(Map.class);
                Long remoteMessageId = null;
                if (data != null) {
                    try {
                        Object mid = data.get("messageId");
                        if (mid instanceof Number) {
                            remoteMessageId = ((Number) mid).longValue();
                        } else if (mid != null) {
                            remoteMessageId = Long.parseLong(String.valueOf(mid));
                        }
                    } catch (Exception ignored) {}
                }
                
                if (remoteMessageId != null) {
                    final Long finalMid = remoteMessageId;
                    
                    // Immediately deduplicate with websocket message
                    AppExecutors.io().execute(() -> {
                        AppDatabase db = AppDatabase.getInstance(this);
                        ChatMessageEntity existed = db.chatMessageDao().getMessageByRemoteId(finalMid);
                        if (existed != null && !existed.getLocalId().equals(localMsg.getLocalId())) {
                            // WebSocket already received it
                            existed.setMessageContent(localMsg.getMessageContent()); // Keep local file path
                            db.chatMessageDao().insertOrReplace(existed);
                            db.chatMessageDao().deleteByLocalId(localMsg.getLocalId());
                            // Update localMsg reference to existed so upload callback updates the right one
                            localMsg.setLocalId(existed.getLocalId());
                        } else {
                            localMsg.setMessageId(finalMid);
                            db.chatMessageDao().insertOrReplace(localMsg);
                        }
                    });

                    Map<String, Object> uploadParams = new HashMap<>();
                    uploadParams.put("messageId", finalMid);
                    
                    Map<String, File> files = new HashMap<>();
                    files.put("file", file);
                    
                    Type uploadType = new TypeToken<Result<Object>>(){}.getType();
                    HttpClient.uploadFiles("/chat/uploadFile", uploadParams, files, token, uploadType, uploadResult -> {
                        AppExecutors.io().execute(() -> {
                            AppDatabase db = AppDatabase.getInstance(this);
                            // Fetch latest by remote messageId in case WebSocket updated it or we merged it
                            ChatMessageEntity currentMsg = db.chatMessageDao().getMessageByRemoteId(finalMid);
                            if (currentMsg == null) currentMsg = localMsg;
                            
                            if (uploadResult != null && "success".equals(uploadResult.getStatus())) {
                                currentMsg.setStatus(1);
                            } else {
                                currentMsg.setStatus(2);
                            }
                            db.chatMessageDao().insertOrReplace(currentMsg);
                        });
                    });
                }
            } else {
                mainHandler.post(() -> Toast.makeText(ChatActivity.this, "发送消息记录失败", Toast.LENGTH_SHORT).show());
                AppExecutors.io().execute(() -> {
                    localMsg.setStatus(2);
                    AppDatabase.getInstance(this).chatMessageDao().insertOrReplace(localMsg);
                });
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (sessionId != null) {
            activeSessionIds.add(sessionId);
        }
    }

    @Override
    protected void onPause() {
        if (sessionId != null) {
            activeSessionIds.remove(sessionId);
        }
        super.onPause();
    }

    /**
     * 判断指定 session 是否为当前用户正在查看的活跃会话
     */
    public static boolean isSessionActive(String sessionId) {
        return sessionId != null && activeSessionIds.contains(sessionId);
    }

    /**
     * 加载更多历史消息（懒加载分页）
     */
    private void loadMoreHistory() {
        if (isLoadingHistory || !hasMoreHistory || sessionId == null) return;
        isLoadingHistory = true;

        // 先查本地 Room 中最旧的消息 ID
        AppExecutors.io().execute(() -> {
            AppDatabase db = AppDatabase.getInstance(this);
            ChatMessageEntity earliest = db.chatMessageDao().getEarliestMessage(sessionId);
            final Long lastMsgId = (earliest != null && earliest.getMessageId() != null && earliest.getMessageId() > 0)
                    ? earliest.getMessageId() : null;

            // 构造请求参数
            java.util.Map<String, Object> params = new java.util.HashMap<>();
            params.put("sessionId", sessionId);
            if (lastMsgId != null) {
                params.put("lastMessageId", lastMsgId);
            }
            params.put("pageSize", HISTORY_PAGE_SIZE);

            java.lang.reflect.Type type = new com.google.gson.reflect.TypeToken<com.example.chatapp.chat.AddFriendRequest.Result<Map<String, Object>>>(){}.getType();
            com.example.chatapp.chat.AddFriendRequest.HttpClient.get(
                    "/chat/loadHistory?sessionId=" + sessionId
                            + (lastMsgId != null ? "&lastMessageId=" + lastMsgId : "")
                            + "&pageSize=" + HISTORY_PAGE_SIZE,
                    type, token, result -> {
                        if (result != null && "success".equals(result.getStatus())) {
                            Map<String, Object> data = result.getDataAs(Map.class);
                            if (data != null) {
                                Object listObj = data.get("list");
                                if (listObj instanceof java.util.List) {
                                    java.util.List<Map<String, Object>> rawList = (java.util.List<Map<String, Object>>) listObj;
                                    if (rawList.isEmpty()) {
                                        hasMoreHistory = false;
                                        isLoadingHistory = false;
                                        return;
                                    }
                                    // 将后端返回的消息转换为本地实体并保存
                                    AppExecutors.io().execute(() -> {
                                        try {
                                            com.google.gson.Gson gson = new com.google.gson.Gson();
                                            String jsonStr = gson.toJson(rawList);
                                            java.lang.reflect.Type listType = new com.google.gson.reflect.TypeToken<java.util.List<ChatMessageEntity>>(){}.getType();
                                            // 手动解析，因为字段名不同
                                            java.util.List<ChatMessageEntity> entities = new java.util.ArrayList<>();
                                            for (Map<String, Object> item : rawList) {
                                                ChatMessageEntity entity = new ChatMessageEntity();
                                                Object mid = item.get("messageId");
                                                if (mid instanceof Number) {
                                                    long msgId = ((Number) mid).longValue();
                                                    entity.setMessageId(msgId);
                                                    entity.setLocalId("remote_" + msgId);
                                                }
                                                entity.setSessionId((String) item.get("sessionId"));
                                                entity.setMessageType(item.get("messageType") instanceof Number ? ((Number) item.get("messageType")).intValue() : null);
                                                entity.setMessageContent((String) item.get("messageContent"));
                                                entity.setSendUserId((String) item.get("sendUserId"));
                                                entity.setSendUserNickName((String) item.get("sendUserNickName"));
                                                if (item.get("sendTime") instanceof Number) {
                                                    entity.setSendTime(((Number) item.get("sendTime")).longValue());
                                                }
                                                entity.setContactId((String) item.get("contactId"));
                                                if (item.get("contactType") instanceof Number) {
                                                    entity.setContactType(((Number) item.get("contactType")).intValue());
                                                }
                                                if (item.get("fileSize") instanceof Number) {
                                                    entity.setFileSize(((Number) item.get("fileSize")).longValue());
                                                }
                                                entity.setFileName((String) item.get("fileName"));
                                                if (item.get("fileType") instanceof Number) {
                                                    entity.setFileType(((Number) item.get("fileType")).intValue());
                                                }
                                                entity.setStatus(1);
                                                entity.setClientOrderTime(0L);

                                                // 去重：跳过本地已存在的消息
                                                if (entity.getMessageId() != null && entity.getMessageId() > 0) {
                                                    ChatMessageEntity existed = db.chatMessageDao().getMessageByRemoteId(entity.getMessageId());
                                                    if (existed != null) continue;
                                                }
                                                entities.add(entity);
                                            }
                                            if (!entities.isEmpty()) {
                                                db.chatMessageDao().insertOrReplaceList(entities);
                                            } else {
                                                hasMoreHistory = false;
                                            }
                                        } catch (Exception e) {
                                            Log.e(TAG, "加载历史消息解析失败", e);
                                        } finally {
                                            isLoadingHistory = false;
                                        }
                                    });
                                    return;
                                }
                            }
                        }
                        isLoadingHistory = false;
                    }
            );
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}