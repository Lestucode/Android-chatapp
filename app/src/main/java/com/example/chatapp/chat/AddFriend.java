package com.example.chatapp.chat;

import static android.content.ContentValues.TAG;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.chatapp.R;
import com.example.chatapp.User;
import com.example.chatapp.UserDao;
import com.example.chatapp.chat.AddFriendRequest.HttpClient;
import com.example.chatapp.chat.AddFriendRequest.Result;
import com.example.chatapp.data.UserContactSearchResultDto;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class AddFriend extends AppCompatActivity {
    private MaterialToolbar toolbar;
    private TextInputEditText etSearch;
    private RecyclerView rvResults;
    private UserSearchAdapter adapter;
    private List<UserContactSearchResultDto> searchResults = new ArrayList<>();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.add_friend);

        initViews();
        initRecyclerView();
        setSearchListener();
    }

    private void initViews() {
        toolbar = findViewById(R.id.add_friend_toolbar);
        etSearch = findViewById(R.id.tiet_friend_id);
        rvResults = findViewById(R.id.rv_search_results);

        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void initRecyclerView() {
        User local = UserDao.getInstance().getUser();
        String jwt = local != null ? local.getToken() : null;

        rvResults.setLayoutManager(new LinearLayoutManager(this));
        adapter = new UserSearchAdapter(searchResults, this, jwt);
        rvResults.setAdapter(adapter);
    }

    private void setSearchListener() {
        etSearch.addTextChangedListener(new TextWatcher() {
            private Handler handler = new Handler(Looper.getMainLooper());
            private Runnable searchRunnable;

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String query = s.toString().trim();

                if (searchRunnable != null) {
                    handler.removeCallbacks(searchRunnable);
                }

                searchRunnable = () -> {
                    if (!query.isEmpty()) {
                        searchUsers(query);
                    } else {
                        searchResults.clear();
                        adapter.notifyDataSetChanged();
                    }
                };
                handler.postDelayed(searchRunnable, 500);
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void searchUsers(String query) {
        User local = UserDao.getInstance().getUser();
        if (local == null || local.getToken() == null) return;

        Type type = new TypeToken<Result<UserContactSearchResultDto>>(){}.getType();
        // contactId 可以作为 URL 参数拼接，注意这里假设 query 就是 contactId
        HttpClient.get("/contact/search?contactId=" + query, type, local.getToken(), result -> {
            runOnUiThread(() -> {
                searchResults.clear();
                if (result != null && "success".equals(result.getStatus())) {
                    UserContactSearchResultDto dto = (UserContactSearchResultDto) result.getData();
                    if (dto != null) {
                        searchResults.add(dto);
                    } else {
                        Toast.makeText(AddFriend.this, "未找到用户", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    String msg = result != null ? result.getInfo() : "请求失败";
                    Toast.makeText(AddFriend.this, msg, Toast.LENGTH_SHORT).show();
                }
                adapter.notifyDataSetChanged();
            });
        });
    }
}