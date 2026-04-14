package com.example.chatapp.chat;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.Fragment;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.model.GlideUrl;
import com.bumptech.glide.load.model.LazyHeaders;
import com.example.chatapp.R;
import com.example.chatapp.User;
import com.example.chatapp.UserDao;
import com.example.chatapp.chat.AddFriendRequest.HttpClient;
import com.example.chatapp.chat.AddFriendRequest.Result;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationView;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.Map;

public class ChatHomePage extends AppCompatActivity {
    private Toolbar toolbar;
    private DrawerLayout drawerLayout;
    private OnBackPressedCallback drawerOnBackPressedCallback;
    private ImageView imageView;
    private TextView nav_nickname;
    private TextView nav_PersonSignature;

    private BroadcastReceiver userInfoUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if ("com.example.chatapp.USER_INFO_UPDATED".equals(intent.getAction())) {
                // 当收到更新广播时，重新加载信息
                loadUserInfo();
            }
        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.nav_homepage);

        // 注册广播监听器
        LocalBroadcastManager.getInstance(this).registerReceiver(
                userInfoUpdateReceiver, 
                new IntentFilter("com.example.chatapp.USER_INFO_UPDATED")
        );

        initView();
        setupOnBackPressed();

        // 初始加载 ChatFragment
        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, new ChatFragment())
                    .commit();
        }
    }

    private void initView() {
        toolbar = findViewById(R.id.home_toolbar);
        setSupportActionBar(toolbar);

        drawerLayout = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.home_navView);
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation_view);
        
        // 获取 NavigationView 的 HeaderView，绑定里面的控件
        View headerView = navigationView.getHeaderView(0);
        imageView = headerView.findViewById(R.id.imageView);
        nav_nickname = headerView.findViewById(R.id.nav_nickname);
        nav_PersonSignature = headerView.findViewById(R.id.nav_PersonSignature);

        // 加载用户信息并显示到侧边栏头部
        loadUserInfo();

        // Drawer toggle
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this,
                drawerLayout,
                toolbar,
                R.string.open_drawer,
                R.string.close_drawer
        );
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        // Drawer item 点击
        navigationView.setNavigationItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.addFriend_item) {
                startActivity(new Intent(ChatHomePage.this, AddFriend.class));
            } else if (itemId == R.id.set_item) {
                startActivity(new Intent(ChatHomePage.this, Set.class));
            } else if(itemId == R.id.friendApply_item) {
                startActivity(new Intent(ChatHomePage.this, NewFriend.class));
            } else if(itemId == R.id.createGroup_item) {
                startActivity(new Intent(ChatHomePage.this, GroupCreat.class));
            } else {
                return false;
            }
            drawerLayout.closeDrawer(GravityCompat.START);
            return true;
        });

        // BottomNavigation 切换 Fragment
        bottomNavigationView.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = null;
            int itemId = item.getItemId();

            if (itemId == R.id.action_page_1) {
                selectedFragment = new ChatFragment();
            } else if (itemId == R.id.action_page_2) {
                selectedFragment = new ContactFragment();
            } else if (itemId == R.id.action_page_3) {
                selectedFragment = new MomentsFragment();
            }

            if (selectedFragment != null) {
                getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.fragment_container, selectedFragment)
                        .commit();
                return true;
            }
            return false;
        });

        // 设置默认选中项
        bottomNavigationView.setSelectedItemId(R.id.action_page_1);
    }
    public void loadUserInfo() {
        User user = UserDao.getInstance().getUser();
        if (user == null || user.getToken() == null) return;

        Type type = new TypeToken<Result<Map<String, Object>>>(){}.getType();
        HttpClient.get("/userInfo/getUserInfo", type, user.getToken(), result -> {
            if (result != null && "success".equals(result.getStatus())) {
                Map<String, Object> info = result.getDataAs(Map.class);
                if (info != null) {
                    runOnUiThread(() -> {
                        Object nickNameObj = info.get("nickName");
                        Object signatureObj = info.get("personalSignature");
                        
                        String nickName = nickNameObj != null ? nickNameObj.toString() : "";
                        String signature = signatureObj != null ? signatureObj.toString() : "这个人很懒，没有留下签名";

                        if (nav_nickname != null) {
                            nav_nickname.setText(nickName);
                        }
                        if (nav_PersonSignature != null) {
                            nav_PersonSignature.setText(signature);
                        }

                        // 加载头像
                        if (imageView != null) {
                            // 增加随机参数 ?t=... 破坏 Glide 的本地缓存，强制加载新头像
                            String avatarUrl = "http://82.157.200.53:5050/api/chat/downloadFile?fileId=" + user.getUserId() + "&showCover=true&t=" + System.currentTimeMillis();
                            GlideUrl glideUrl = new GlideUrl(avatarUrl, new LazyHeaders.Builder()
                                    .addHeader("token", user.getToken())
                                    .build());
                            Glide.with(ChatHomePage.this)
                                    .load(glideUrl)
                                    .placeholder(R.drawable.dinosaur) // 使用默认占位图
                                    .error(R.drawable.dinosaur)
                                    .circleCrop()
                                    .into(imageView);
                        }
                    });
                }
            }
        });
    }
    private void setupOnBackPressed() {
        drawerOnBackPressedCallback = new OnBackPressedCallback(false) {
            @Override
            public void handleOnBackPressed() {
                if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    drawerLayout.closeDrawer(GravityCompat.START);
                } else {
                    setEnabled(false);
                    finish();
                }
            }
        };
        getOnBackPressedDispatcher().addCallback(this, drawerOnBackPressedCallback);

        drawerLayout.addDrawerListener(new DrawerLayout.SimpleDrawerListener() {
            @Override
            public void onDrawerOpened(View drawerView) {
                drawerOnBackPressedCallback.setEnabled(true);
            }

            @Override
            public void onDrawerClosed(View drawerView) {
                drawerOnBackPressedCallback.setEnabled(false);
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
                drawerLayout.closeDrawer(GravityCompat.START);
            } else {
                drawerLayout.openDrawer(GravityCompat.START);
            }
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 解绑广播监听器
        LocalBroadcastManager.getInstance(this).unregisterReceiver(userInfoUpdateReceiver);
    }
}
