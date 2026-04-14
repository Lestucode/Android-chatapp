package com.example.chatapp.chat;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter; // 确保导入
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.localbroadcastmanager.content.LocalBroadcastManager; // 确保导入

import com.example.chatapp.R;
import com.example.chatapp.chat.ConversationItem; // 确保导入

import org.json.JSONException;
import org.json.JSONObject;

import com.example.chatapp.MainActivity;
import com.example.chatapp.UserDao;
import com.example.chatapp.WebSocketService;
import com.example.chatapp.data.MessageSendDto;
import com.google.gson.Gson;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.example.chatapp.database.AppDatabase;
import androidx.recyclerview.widget.ItemTouchHelper;
import com.example.chatapp.util.AppExecutors;
import com.example.chatapp.database.ChatSessionEntity;

public class ChatFragment extends Fragment {
    private static final String TAG = "ChatFragment";

    private RecyclerView rvConversations;
    private List<ConversationItem> conversationsList;

    private Gson gson = new Gson();

    private final BroadcastReceiver wsReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (WebSocketService.ACTION_WS_MESSAGE.equals(action)) {
                String payload = intent.getStringExtra(WebSocketService.EXTRA_MESSAGE);
                try {
                    if (!isAdded() || getContext() == null) return;
                    
                    MessageSendDto<?> base = gson.fromJson(payload, MessageSendDto.class);
                    if (base == null || base.getMessageType() == null) return;

                    int mt = base.getMessageType();
                    Log.d(TAG, "收到服务器消息，类型值: " + mt);
                    
                    switch (mt) {
                        case 7: { // FORCE_OFF_LINE
                            Log.w(TAG, "收到强制下线通知");
                            UserDao.getInstance().clearUser();
                            Context ctx = getContext();
                            if (ctx != null) {
                                ctx.stopService(new Intent(ctx, WebSocketService.class));
                                Intent login = new Intent(ctx, MainActivity.class)
                                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                startActivity(login);
                            }
                            if (getActivity() != null) {
                                getActivity().finish();
                            }
                            break;
                        }
                        default:
                            Log.d(TAG, "收到其他类型消息: " + mt);
                            break;
                    }
                } catch (Exception e) {
                    Log.e(TAG, "解析 WebSocket 消息失败: " + payload, e);
                }
            } else if (WebSocketService.ACTION_WS_STATUS.equals(action)) {
                String status = intent.getStringExtra(WebSocketService.EXTRA_STATUS);
                Log.d(TAG, "WebSocket 状态更新: " + status);
                // TODO: 可以在 UI 上方显示连接状态，比如 "正在重连..."
            }
        }
    };

    private ChatSessionAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_chat_list, container, false);
        
        rvConversations = view.findViewById(R.id.rv_chat_list);
        rvConversations.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new ChatSessionAdapter(requireContext());
        rvConversations.setAdapter(adapter);

        // 监听 Room 数据库的会话列表变化
        AppDatabase.getInstance(requireContext()).chatSessionDao().getAllSessions().observe(getViewLifecycleOwner(), sessions -> {
            if (sessions != null) {
                adapter.submitList(sessions);
            }
        });

        // 添加左滑删除功能
        ItemTouchHelper.SimpleCallback swipeCallback = new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();
                ChatSessionEntity session = adapter.getCurrentList().get(position);
                if (session != null) {
                    AppExecutors.io().execute(() -> {
                        AppDatabase db = AppDatabase.getInstance(requireContext());
                        db.chatSessionDao().deleteSessionById(session.getSessionId());
                        db.chatMessageDao().deleteMessagesBySessionId(session.getSessionId());
                    });
                }
            }
        };
        new ItemTouchHelper(swipeCallback).attachToRecyclerView(rvConversations);

        return view;
    }

    @Override
    public void onStart() {
        super.onStart();
        // 注册广播接收器
        IntentFilter filter = new IntentFilter();
        filter.addAction(WebSocketService.ACTION_WS_MESSAGE);
        filter.addAction(WebSocketService.ACTION_WS_STATUS);
        LocalBroadcastManager.getInstance(requireContext()).registerReceiver(wsReceiver, filter);
    }

    @Override
    public void onStop() {
        super.onStop();
        // 注销广播接收器
        LocalBroadcastManager.getInstance(requireContext()).unregisterReceiver(wsReceiver);
    }

}
