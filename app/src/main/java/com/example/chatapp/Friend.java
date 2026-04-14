package com.example.chatapp;

import kotlin.text.StringsKt;

public class Friend {
    private String userId;        // 用户ID（必须）
    private String displayName;   // 显示名称（必须）
    private String avatar;        // 头像URL
    private String signature;     // 个性签名
    private String remark;        // 好友备注

    public Friend(String userId, String displayName, String avatar, String signature, String remark) {
        this.userId = userId;
        this.displayName = displayName;
        this.avatar = avatar;
        this.signature = signature;
        this.remark = remark;
    }
    public Friend(){
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getAvatar() {
        return avatar;
    }

    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }

    public String getSignature() {
        return signature;
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}
