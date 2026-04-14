package com.example.chatapp.chat;

import java.util.List;

public class ResultListUser {
    private int code;
    private String msg;
    private List<SearchUser> data;

    public int getCode() { return code; }
    public String getMsg() { return msg; }
    public List<SearchUser> getData() { return data; }
}
