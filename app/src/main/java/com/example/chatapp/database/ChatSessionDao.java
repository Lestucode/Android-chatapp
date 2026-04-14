package com.example.chatapp.database;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface ChatSessionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertOrReplace(ChatSessionEntity session);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertOrReplaceList(List<ChatSessionEntity> sessions);

    @Update
    void update(ChatSessionEntity session);

    // 获取按时间降序排列的会话列表，LiveData用于UI自动刷新
    @Query("SELECT * FROM chat_session ORDER BY last_receive_time DESC")
    LiveData<List<ChatSessionEntity>> getAllSessions();

    @Query("SELECT * FROM chat_session WHERE session_id = :sessionId LIMIT 1")
    ChatSessionEntity getSessionById(String sessionId);

    // 清空未读数
    @Query("UPDATE chat_session SET unread_count = 0 WHERE session_id = :sessionId")
    void clearUnreadCount(String sessionId);

    // 收到消息时，增加未读数并更新最后一条消息内容与时间
    @Query("UPDATE chat_session SET unread_count = unread_count + 1, last_message = :lastMsg, last_receive_time = :time WHERE session_id = :sessionId")
    void updateUnreadAndLastMsg(String sessionId, String lastMsg, Long time);

    @Query("UPDATE chat_session SET last_message = :lastMsg, last_receive_time = :time WHERE session_id = :sessionId")
    void updateLastMsg(String sessionId, String lastMsg, Long time);
    
    @Query("UPDATE chat_session SET contact_name = :contactName WHERE contact_id = :contactId")
    void updateContactName(String contactId, String contactName);
    
    @Query("DELETE FROM chat_session WHERE session_id = :sessionId")
    void deleteSessionById(String sessionId);
    
    // 清空所有会话（退出登录用）
    @Query("DELETE FROM chat_session")
    void clearAll();
}
