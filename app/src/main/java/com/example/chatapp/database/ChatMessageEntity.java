package com.example.chatapp.database;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.annotation.NonNull;
import java.util.UUID;

@Entity(tableName = "chat_message")
public class ChatMessageEntity {

    @PrimaryKey
    @NonNull
    @ColumnInfo(name = "local_id")
    private String localId = UUID.randomUUID().toString(); // 客户端自己生成的唯一ID

    @ColumnInfo(name = "message_id")
    private Long messageId; // 服务端下发的唯一ID（可能为null如果还在发送中）

    @ColumnInfo(name = "session_id")
    private String sessionId;

    @ColumnInfo(name = "message_type")
    private Integer messageType;

    @ColumnInfo(name = "message_content")
    private String messageContent;

    @ColumnInfo(name = "send_user_id")
    private String sendUserId;

    @ColumnInfo(name = "send_user_nick_name")
    private String sendUserNickName;

    @ColumnInfo(name = "send_time")
    private Long sendTime;

    @ColumnInfo(name = "client_order_time")
    private Long clientOrderTime;

    @ColumnInfo(name = "contact_id")
    private String contactId;

    @ColumnInfo(name = "contact_type")
    private Integer contactType; // 0:单聊 1:群聊

    @ColumnInfo(name = "file_size")
    private Long fileSize;

    @ColumnInfo(name = "file_name")
    private String fileName;

    @ColumnInfo(name = "file_type")
    private Integer fileType;

    @ColumnInfo(name = "status")
    private Integer status; // 0:发送中 1:已发送

    @NonNull
    public String getLocalId() {
        return localId;
    }

    public void setLocalId(@NonNull String localId) {
        this.localId = localId;
    }

    public Long getMessageId() {
        return messageId;
    }

    public void setMessageId(Long messageId) {
        this.messageId = messageId;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public Integer getMessageType() {
        return messageType;
    }

    public void setMessageType(Integer messageType) {
        this.messageType = messageType;
    }

    public String getMessageContent() {
        return messageContent;
    }

    public void setMessageContent(String messageContent) {
        this.messageContent = messageContent;
    }

    public String getSendUserId() {
        return sendUserId;
    }

    public void setSendUserId(String sendUserId) {
        this.sendUserId = sendUserId;
    }

    public String getSendUserNickName() {
        return sendUserNickName;
    }

    public void setSendUserNickName(String sendUserNickName) {
        this.sendUserNickName = sendUserNickName;
    }

    public Long getSendTime() {
        return sendTime;
    }

    public void setSendTime(Long sendTime) {
        this.sendTime = sendTime;
    }

    public Long getClientOrderTime() {
        return clientOrderTime;
    }

    public void setClientOrderTime(Long clientOrderTime) {
        this.clientOrderTime = clientOrderTime;
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

    public Long getFileSize() {
        return fileSize;
    }

    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public Integer getFileType() {
        return fileType;
    }

    public void setFileType(Integer fileType) {
        this.fileType = fileType;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }
}
