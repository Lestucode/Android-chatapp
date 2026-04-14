package com.example.chatapp.chat;

import java.util.Date;

public class ConversationItem {
    private long friendId;        // 朋友/会话的唯一ID
    private String friendName;    // 朋友的昵称
    private String avatarUrl;     // 朋友的头像 URL
    private String lastMessage;   // 最后一条消息的内容 (对应 friend_last_msg)
    private Date lastTime;        // 最后一条消息的时间 (对应 friend_time)
    private int unreadCount;      // 未读消息数量 (对应 unread_badge_text)
    private boolean isFriendRequest = false;
    // 构造函数 (Constructor)
    public ConversationItem() {
        // 默认构造函数
    }


    public long getFriendId() {
        return friendId;
    }
    public void setFriendId(long friendId) {
        this.friendId = friendId;
    }

    // 例子：获取/设置是否是好友请求
    public boolean isFriendRequest() {
        return isFriendRequest;
    }
    public void setFriendRequest(boolean friendRequest) {
        isFriendRequest = friendRequest;
    }

    public String getFriendName() {
        return friendName;
    }

    public void setFriendName(String friendName) {
        this.friendName = friendName;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public String getLastMessage() {
        return lastMessage;
    }

    public void setLastMessage(String lastMessage) {
        this.lastMessage = lastMessage;
    }

    public Date getLastTime() {
        return lastTime;
    }

    public void setLastTime(Date lastTime) {
        this.lastTime = lastTime;
    }

    public int getUnreadCount() {
        return unreadCount;
    }

    public void setUnreadCount(int unreadCount) {
        this.unreadCount = unreadCount;
    }
}
