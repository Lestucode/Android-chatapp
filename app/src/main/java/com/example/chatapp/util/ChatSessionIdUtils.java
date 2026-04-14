package com.example.chatapp.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;

public final class ChatSessionIdUtils {
    private ChatSessionIdUtils() {
    }

    public static String getChatSessionId4User(String userId1, String userId2) {
        if (userId1 == null || userId2 == null) {
            return null;
        }
        String[] userIds = new String[]{userId1, userId2};
        Arrays.sort(userIds);
        return md5(userIds[0] + userIds[1]);
    }

    public static String getChatSessionId4Group(String groupId) {
        if (groupId == null) {
            return null;
        }
        return md5(groupId);
    }

    public static Integer getContactTypeById(String contactId) {
        if (contactId == null || contactId.isEmpty()) {
            return null;
        }
        char prefix = contactId.charAt(0);
        if (prefix == 'G') {
            return 1;
        }
        if (prefix == 'U') {
            return 0;
        }
        return null;
    }

    private static String md5(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            return null;
        }
    }
}
