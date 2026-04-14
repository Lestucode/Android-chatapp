package com.example.chatapp.chat.AddFriendRequest;

public class Result<T> {
    private String status;
    private Integer code;
    private String info;
    private T data;

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Integer getCode() { return code; }
    public void setCode(Integer code) { this.code = code; }
    public String getInfo() { return info; }
    public void setInfo(String info) { this.info = info; }
    public T getData() { return data; }
    public void setData(T data) { this.data = data; }
    
    // 添加类型转换方法
    @SuppressWarnings("unchecked")
    public <E> E getDataAs(Class<E> clazz) {
        return (E) data;
    }
}
