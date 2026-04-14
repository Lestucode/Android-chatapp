package com.example.chatapp.chat;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.chatapp.R;
import com.example.chatapp.UserDao;
import com.example.chatapp.WebSocketService;
import com.example.chatapp.chat.AddFriendRequest.HttpClient;
import com.example.chatapp.chat.AddFriendRequest.Result;
import com.example.chatapp.data.MessageSendDto;
import com.example.chatapp.data.PaginationResultVO;
import com.example.chatapp.data.UserContactApplyDto;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class NewFriend extends AppCompatActivity {
    private static final String TAG = "NewFriend";
    private RecyclerView recyclerView;
    private NewFriendAdapter adapter;
    private List<UserContactApplyDto> applyList = new ArrayList<>();
    private int pageNo = 1;

    private final BroadcastReceiver wsReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (WebSocketService.ACTION_WS_MESSAGE.equals(action)) {
                String payload = intent.getStringExtra(WebSocketService.EXTRA_MESSAGE);
                try {
                    MessageSendDto<?> base = new Gson().fromJson(payload, MessageSendDto.class);
                    if (base != null && base.getMessageType() != null) {
                        int mt = base.getMessageType();
                        if (mt == 4 || mt == 0) { // 4: ADD_FRIEND, 0: INIT (may contain offline applyCount)
                            pageNo = 1;
                            loadApplies();
                        }
                    }
                } catch (Exception e) {
                    Log.e(TAG, "解析 WebSocket 消息失败", e);
                }
            } else if (WebSocketService.ACTION_WS_RECONNECTED.equals(action)) {
                pageNo = 1;
                loadApplies();
            }
        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.newfriend);

        MaterialToolbar toolbar = findViewById(R.id.newfirend);
        toolbar.setNavigationOnClickListener(v -> finish());

        recyclerView = findViewById(R.id.newfriend_item);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new NewFriendAdapter(this, applyList);
        recyclerView.setAdapter(adapter);

        IntentFilter filter = new IntentFilter();
        filter.addAction(WebSocketService.ACTION_WS_MESSAGE);
        filter.addAction(WebSocketService.ACTION_WS_RECONNECTED);
        LocalBroadcastManager.getInstance(this).registerReceiver(wsReceiver, filter);

        loadApplies();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(wsReceiver);
    }

    private void loadApplies() {
        String token = UserDao.getInstance().getUser() != null ? UserDao.getInstance().getUser().getToken() : "";
        Type type = new TypeToken<Result<PaginationResultVO<UserContactApplyDto>>>(){}.getType();
        
        HttpClient.get("/contact/loadApply?pageNo=" + pageNo, type, token, result -> {
            runOnUiThread(() -> {
                if (result != null && "success".equals(result.getStatus())) {
                    PaginationResultVO<UserContactApplyDto> pagination = (PaginationResultVO<UserContactApplyDto>) result.getData();
                    if (pagination != null && pagination.getList() != null) {
                        if (pageNo == 1) {
                            applyList.clear();
                        }
                        applyList.addAll(pagination.getList());
                        adapter.notifyDataSetChanged();
                    }
                } else {
                    String msg = result != null ? result.getInfo() : "加载好友申请失败";
                    Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
                }
            });
        });
    }
}
