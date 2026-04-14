package com.example.chatapp;

public class AppConfig {

    public static final String SERVER_IP = "";


    public static final String HTTP_BASE_URL = "";


    public static final String WS_BASE_URL = "";

    public static final String FILE_BASE_URL = HTTP_BASE_URL + "";
}

2026 4/14
BUG待修复：
  1.退出登陆时清空了room，再次登陆未拉取数据库同步，导致聊天记录丢失
  2.退出到二次登录期间，图片出现丢失（群聊，单聊暂未测试）
  3.需要修改后端的图片、视频、文件的获取方式（获取失败的原因可能是安卓显示http明文下载）
功能待完成:
  1.朋友圈功能
  2.后端与安卓端的推送SDK同步
