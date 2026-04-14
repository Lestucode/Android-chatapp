package com.example.chatapp.database;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.annotation.NonNull;

@Entity(tableName = "chat_session")
public class ChatSessionEntity {

    @PrimaryKey
    @NonNull
    @ColumnInfo(name = "session_id")
    private String sessionId;

    @ColumnInfo(name = "contact_id")
    private String contactId;

    @ColumnInfo(name = "contact_type")
    private Integer contactType; // 0:单聊 1:群聊

    @ColumnInfo(name = "contact_name")
    private String contactName;

    @ColumnInfo(name = "last_message")
    private String lastMessage;

    @ColumnInfo(name = "last_receive_time")
    private Long lastReceiveTime;

    @ColumnInfo(name = "unread_count")
    private Integer unreadCount = 0;

    @ColumnInfo(name = "member_count")
    private Integer memberCount = 0;

    @NonNull
    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(@NonNull String sessionId) {
        this.sessionId = sessionId;
    }

    public String getContactId() {
        return contactId;
    }

    public void setContactId(String contactId) {
        this.contactId = contactId;
    }

    public Integer getContactType() {
        return contactType;
    }

    public void setContactType(Integer contactType) {
        this.contactType = contactType;
    }

    public String getContactName() {
        return contactName;
    }

    public void setContactName(String contactName) {
        this.contactName = contactName;
    }

    public String getLastMessage() {
        return lastMessage;
    }

    public void setLastMessage(String lastMessage) {
        this.lastMessage = lastMessage;
    }

    public Long getLastReceiveTime() {
        return lastReceiveTime;
    }

    public void setLastReceiveTime(Long lastReceiveTime) {
        this.lastReceiveTime = lastReceiveTime;
    }

    public Integer getUnreadCount() {
        return unreadCount;
    }

    public void setUnreadCount(Integer unreadCount) {
        this.unreadCount = unreadCount;
    }

    public Integer getMemberCount() {
        return memberCount;
    }

    public void setMemberCount(Integer memberCount) {
        this.memberCount = memberCount;
    }
}
