package com.example.chatapp.data;

public class UserContactApplyDto {
    private Integer applyId;
    private String applyUserId;
    private String receiveUserId;
    private Integer contactType;
    private String contactId;
    private Long lastApplyTime;
    private Integer status;
    private String applyInfo;
    private String contactName;
    private String statusName;

    public Integer getApplyId() { return applyId; }
    public void setApplyId(Integer applyId) { this.applyId = applyId; }

    public String getApplyUserId() { return applyUserId; }
    public void setApplyUserId(String applyUserId) { this.applyUserId = applyUserId; }

    public String getReceiveUserId() { return receiveUserId; }
    public void setReceiveUserId(String receiveUserId) { this.receiveUserId = receiveUserId; }

    public Integer getContactType() { return contactType; }
    public void setContactType(Integer contactType) { this.contactType = contactType; }

    public String getContactId() { return contactId; }
    public void setContactId(String contactId) { this.contactId = contactId; }

    public Long getLastApplyTime() { return lastApplyTime; }
    public void setLastApplyTime(Long lastApplyTime) { this.lastApplyTime = lastApplyTime; }

    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }

    public String getApplyInfo() { return applyInfo; }
    public void setApplyInfo(String applyInfo) { this.applyInfo = applyInfo; }

    public String getContactName() { return contactName; }
    public void setContactName(String contactName) { this.contactName = contactName; }

    public String getStatusName() { return statusName; }
    public void setStatusName(String statusName) { this.statusName = statusName; }
}
