package com.example.chatapp.chat;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

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

    private static volatile String activeSessionId;
    
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
                    files.put("cover", file);
                    
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
        activeSessionId = sessionId;
    }

    @Override
    protected void onPause() {
        if (sessionId != null && sessionId.equals(activeSessionId)) {
            activeSessionId = null;
        }
        super.onPause();
    }

    public static String getActiveSessionId() {
        return activeSessionId;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}