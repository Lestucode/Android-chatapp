package com.example.chatapp.data;

import java.util.List;

public class WsInitData {
    private List<ChatSessionUser> chatSessionList;
    private List<ChatMessageDto> chatMessageList;
    private Integer applyCount;

    public List<ChatSessionUser> getChatSessionList() {
        return chatSessionList;
    }

    public void setChatSessionList(List<ChatSessionUser> chatSessionList) {
        this.chatSessionList = chatSessionList;
    }

    public List<ChatMessageDto> getChatMessageList() {
        return chatMessageList;
    }

    public void setChatMessageList(List<ChatMessageDto> chatMessageList) {
        this.chatMessageList = chatMessageList;
    }

    public Integer getApplyCount() {
        return applyCount;
    }

    public void setApplyCount(Integer applyCount) {
        this.applyCount = applyCount;
    }

    public static class ChatSessionUser {
        private String sessionId;
        private String contactId;
        private String contactName;
        private String lastMessage;
        private Long lastReceiveTime;
        private Integer memberCount;

        public String getSessionId() {
            return sessionId;
        }

        public void setSessionId(String sessionId) {
            this.sessionId = sessionId;
        }

        public String getContactId() {
            return contactId;
        }

        public void setContactId(String contactId) {
            this.contactId = contactId;
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

        public Integer getMemberCount() {
            return memberCount;
        }

        public void setMemberCount(Integer memberCount) {
            this.memberCount = memberCount;
        }
    }

    public static class ChatMessageDto {
        private Long messageId;
        private String sessionId;
        private Integer messageType;
        private String messageContent;
        private String sendUserId;
        private String sendUserNickName;
        private Long sendTime;
        private String contactId;
        private Integer contactType;
        private Long fileSize;
        private String fileName;
        private Integer fileType;
        private Integer status;

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
}
