package com.example.chatapp.data;

public class MessageSendDto<T> {
    private Long messageId;
    private String sessionId;
    private String sendUserId;
    private String sendUserNickName;
    private String contactId;
    private String contactName;
    private String messageContent;
    private Integer messageType;
    private Long sendTime;
    private Integer contactType;
    private Long fileSize;
    private String fileName;
    private Integer fileType;
    private Integer status;
    private String lastMessage;
    private T extendData;

    public Long getMessageId(){return messageId;}
    public String getSessionId(){return sessionId;}
    public String getSendUserId(){return sendUserId;}
    public String getSendUserNickName(){return sendUserNickName;}
    public String getContactId(){return contactId;}
    public String getContactName(){return contactName;}
    public String getMessageContent(){return messageContent;}
    public Integer getMessageType(){return messageType;}
    public Long getSendTime(){return sendTime;}
    public Integer getContactType(){return contactType;}
    
    public Long getFileSize(){return fileSize;}
    public String getFileName(){return fileName;}
    public Integer getFileType(){return fileType;}
    public Integer getStatus(){return status;}
    public String getLastMessage(){return lastMessage;}
    
    public T getExtendData(){return extendData;}

    public void setMessageId(Long v){messageId=v;}
    public void setSessionId(String v){sessionId=v;}
    public void setSendUserId(String v){sendUserId=v;}
    public void setSendUserNickName(String v){sendUserNickName=v;}
    public void setContactId(String v){contactId=v;}
    public void setContactName(String v){contactName=v;}
    public void setMessageContent(String v){messageContent=v;}
    public void setMessageType(Integer v){messageType=v;}
    public void setSendTime(Long v){sendTime=v;}
    public void setContactType(Integer v){contactType=v;}
    
    public void setFileSize(Long v){fileSize=v;}
    public void setFileName(String v){fileName=v;}
    public void setFileType(Integer v){fileType=v;}
    public void setStatus(Integer v){status=v;}
    public void setLastMessage(String v){lastMessage=v;}
    
    public void setExtendData(T v){extendData=v;}
}
