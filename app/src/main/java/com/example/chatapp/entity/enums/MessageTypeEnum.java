package com.example.chatapp.entity.enums;

public enum MessageTypeEnum {
    INIT(0, "连接初始化"),
    ADD_FRIEND(1, "添加好友"),
    CHAT(2, "普通聊天消息"),
    GROUP_CREATE(3, "群组创建"),
    CONTACT_APPLY(4, "联系人申请"),
    MEDIA_CHAT(5, "媒体消息"),
    FILE_UPLOAD(6, "文件上传"),
    FORCE_OFF_LINE(7, "强制下线"),
    DISSOLVE_GROUP(8, "解散群聊"),
    ADD_GROUP(9, "加入群聊"),
    CONTACT_NAME_UPDATE(10, "更新联系人名称"),
    LEAVE_GROUP(11, "退出群聊"),
    REMOVE_GROUP(12, "踢出群聊"),
    ADD_FRIEND_SELF(13, "添加好友(自己)"),
    UNKNOWN(-1, "未知消息");

    private final Integer type;
    private final String desc;

    MessageTypeEnum(Integer type, String desc) {
        this.type = type;
        this.desc = desc;
    }

    public Integer getType() {
        return type;
    }

    public String getDesc() {
        return desc;
    }

    public static MessageTypeEnum getByType(Integer type) {
        if (type == null) {
            return UNKNOWN;
        }
        for (MessageTypeEnum item : MessageTypeEnum.values()) {
            if (item.getType().equals(type)) {
                return item;
            }
        }
        return UNKNOWN;
    }
}
