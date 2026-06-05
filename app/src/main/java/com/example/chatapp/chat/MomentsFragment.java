package com.example.chatapp.chat;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.OptIn;
import androidx.fragment.app.Fragment;
import androidx.media3.common.util.UnstableApi;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.example.chatapp.AppConfig;
import com.example.chatapp.R;
import com.example.chatapp.chat.AddFriendRequest.HttpClient;
import com.example.chatapp.chat.AddFriendRequest.Result;
import com.example.chatapp.data.MomentCommentVO;
import com.example.chatapp.data.MomentLikeResultVO;
import com.example.chatapp.data.MomentVO;
import com.example.chatapp.data.PaginationResultVO;
import com.google.android.material.button.MaterialButton;
import com.google.gson.reflect.TypeToken;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MomentsFragment extends Fragment implements MomentAdapter.OnMomentClickListener {

    private SwipeRefreshLayout swipeRefreshLayout;
    private RecyclerView rvMoments;
    private ImageView ivPublish;
    private MomentAdapter adapter;
    private List<MomentVO> momentList = new ArrayList<>();
    private String token;
    private String currentUserId;
    private int pageNo = 1;
    private final int pageSize = 10;
    private boolean isLoading = false;

    // 底部评论输入框相关
    private LinearLayout commentLayout;
    private EditText etComment;
    private MaterialButton btnSendComment;
    private MomentVO currentMoment;
    private int currentPosition;

    private final androidx.activity.result.ActivityResultLauncher<Intent> publishLauncher =
            registerForActivityResult(new androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == android.app.Activity.RESULT_OK) {
                    loadFeed(true);
                }
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_moments, container, false);

        SharedPreferences prefs = requireContext().getSharedPreferences("ChatApp", Context.MODE_PRIVATE);
        token = com.example.chatapp.UserDao.getInstance().getUser() != null ? com.example.chatapp.UserDao.getInstance().getUser().getToken() : prefs.getString("token", "");
        currentUserId = com.example.chatapp.UserDao.getInstance().getUser() != null ? com.example.chatapp.UserDao.getInstance().getUser().getUserId() : prefs.getString("userId", "");

        initViews(view);
        loadFeed(true);
        return view;
    }

    private void initViews(View view) {
        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout);
        rvMoments = view.findViewById(R.id.rvMoments);
        ivPublish = view.findViewById(R.id.ivPublish);

        // 初始化底部评论输入框
        commentLayout = view.findViewById(R.id.commentLayout);
        etComment = view.findViewById(R.id.etComment);
        btnSendComment = view.findViewById(R.id.btnSendComment);

        adapter = new MomentAdapter(requireContext(), momentList, currentUserId, this);
        rvMoments.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvMoments.setAdapter(adapter);

        swipeRefreshLayout.setOnRefreshListener(() -> loadFeed(true));

        rvMoments.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                LinearLayoutManager layoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
                if (layoutManager != null && !isLoading) {
                    int lastVisibleItemPosition = layoutManager.findLastVisibleItemPosition();
                    int totalItemCount = layoutManager.getItemCount();
                    if (lastVisibleItemPosition >= totalItemCount - 2) {
                        loadFeed(false);
                    }
                }
            }
        });

        ivPublish.setOnClickListener(v -> {
            Intent intent = new Intent(requireContext(), MomentPublishActivity.class);
            publishLauncher.launch(intent);
        });

        // 发送评论按钮点击事件
        btnSendComment.setOnClickListener(v -> {
            String content = etComment.getText().toString().trim();
            if (!content.isEmpty() && currentMoment != null) {
                submitComment(currentMoment, currentPosition, content);
                // 隐藏评论输入框
                hideCommentLayout();
            }
        });

        // 点击RecyclerView区域隐藏评论输入框
        rvMoments.setOnClickListener(v -> hideCommentLayout());
    }

    @Override
    public void onResume() {
        super.onResume();
        // Maybe refresh if coming back from publish?
        // Could be optimized to only refresh if needed
    }

    public void loadFeed(boolean isRefresh) {
        if (isLoading) return;
        isLoading = true;

        if (isRefresh) {
            pageNo = 1;
        } else {
            pageNo++;
        }

        Map<String, Object> params = new HashMap<>();
        params.put("pageNo", pageNo);
        params.put("pageSize", pageSize);

        HttpClient.post("/moment/loadFeed", params, token,
                new TypeToken<Result<PaginationResultVO<MomentVO>>>() {}.getType(),
                (Result<PaginationResultVO<MomentVO>> result) -> {
                    if (getActivity() == null) return;
                    getActivity().runOnUiThread(() -> {
                        isLoading = false;
                        swipeRefreshLayout.setRefreshing(false);

                        if (result != null && result.getCode() == 200 && result.getData() != null) {
                            if (isRefresh) {
                                momentList.clear();
                            }
                            if (result.getData().getList() != null) {
                                // Prevent duplicates by checking momentId
                                for (MomentVO newMoment : result.getData().getList()) {
                                    boolean exists = false;
                                    for (MomentVO existingMoment : momentList) {
                                        if (existingMoment.getMomentId().equals(newMoment.getMomentId())) {
                                            exists = true;
                                            break;
                                        }
                                    }
                                    if (!exists) {
                                        momentList.add(newMoment);
                                    }
                                }
                            }
                            adapter.notifyDataSetChanged();
                        } else {
                            if (!isRefresh) {
                                pageNo--;
                            }
                            String msg = result != null ? result.getInfo() : "Load failed";
                            Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show();
                        }
                    });
                });
    }

    @Override
    public void onLikeClick(MomentVO moment, int position) {
        Map<String, Object> params = new HashMap<>();
        params.put("momentId", moment.getMomentId());

        HttpClient.post("/moment/like", params, token,
                new TypeToken<Result<MomentLikeResultVO>>() {}.getType(),
                (Result<MomentLikeResultVO> result) -> {
                    if (getActivity() == null) return;
                    getActivity().runOnUiThread(() -> {
                        if (result != null && result.getCode() == 200 && result.getData() != null) {
                            moment.setLiked(result.getData().getLiked());
                            moment.setLikeCount(result.getData().getLikeCount());
                            adapter.notifyItemChanged(position);
                        } else {
                            Toast.makeText(requireContext(), "Action failed", Toast.LENGTH_SHORT).show();
                        }
                    });
                });
    }

    @Override
    public void onCommentClick(MomentVO moment, int position) {
        // 显示底部评论输入框
        currentMoment = moment;
        currentPosition = position;
        commentLayout.setVisibility(View.VISIBLE);
        etComment.requestFocus();
        // 弹出软键盘
        android.view.inputmethod.InputMethodManager imm = (android.view.inputmethod.InputMethodManager) requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.showSoftInput(etComment, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT);
    }

    /**
     * 隐藏底部评论输入框
     */
    private void hideCommentLayout() {
        commentLayout.setVisibility(View.GONE);
        etComment.setText("");
        currentMoment = null;
        currentPosition = -1;
        // 隐藏软键盘
        android.view.inputmethod.InputMethodManager imm = (android.view.inputmethod.InputMethodManager) requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(etComment.getWindowToken(), 0);
    }

    private void submitComment(MomentVO moment, int position, String content) {
        Map<String, Object> params = new HashMap<>();
        params.put("momentId", moment.getMomentId());
        params.put("content", content);

        HttpClient.post("/moment/comment", params, token,
                new TypeToken<Result<MomentCommentVO>>() {}.getType(),
                (Result<MomentCommentVO> result) -> {
                    if (getActivity() == null) return;
                    getActivity().runOnUiThread(() -> {
                        if (result != null && result.getCode() == 200 && result.getData() != null) {
                            if (moment.getCommentList() == null) {
                                moment.setCommentList(new ArrayList<>());
                            }
                            moment.getCommentList().add(result.getData());
                            moment.setCommentCount((moment.getCommentCount() != null ? moment.getCommentCount() : 0) + 1);
                            adapter.notifyItemChanged(position);
                            Toast.makeText(requireContext(), "Commented successfully", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(requireContext(), "Comment failed", Toast.LENGTH_SHORT).show();
                        }
                    });
                });
    }

    @Override
    public void onDeleteClick(MomentVO moment, int position) {
        new com.google.android.material.dialog.MaterialAlertDialogBuilder(requireContext(), com.google.android.material.R.style.ThemeOverlay_MaterialComponents_Dialog_Alert)
                .setTitle("提示")
                .setMessage("确定要删除这条朋友圈吗？")
                .setPositiveButton("删除", (dialog, which) -> {
                    Map<String, Object> params = new HashMap<>();
                    params.put("momentId", moment.getMomentId());

                    HttpClient.post("/moment/delete", params, token,
                            new TypeToken<Result<Object>>() {}.getType(),
                            (Result<Object> result) -> {
                                if (getActivity() == null) return;
                                getActivity().runOnUiThread(() -> {
                                    if (result != null && result.getCode() == 200) {
                                        momentList.remove(position);
                                        adapter.notifyItemRemoved(position);
                                        adapter.notifyItemRangeChanged(position, momentList.size());
                                        Toast.makeText(requireContext(), "删除成功", Toast.LENGTH_SHORT).show();
                                    } else {
                                        Toast.makeText(requireContext(), "删除失败", Toast.LENGTH_SHORT).show();
                                    }
                                });
                            });
                })
                .setNegativeButton("取消", null)
                .show();
    }

    @OptIn(markerClass = UnstableApi.class)
    @Override
    public void onImageClick(List<String> imageUrls, int position) {
        Intent intent = new Intent(requireContext(), MediaPreviewActivity.class);
        intent.putExtra("mediaUrl", AppConfig.MOMENT_IMAGE_BASE_URL + imageUrls.get(position));
        intent.putExtra("type", "image");
        startActivity(intent);
    }
}