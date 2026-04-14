package com.example.chatapp.database;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface ChatMessageDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertOrReplace(ChatMessageEntity message);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertOrReplaceList(List<ChatMessageEntity> messages);

    // 根据服务端 messageId 获取消息
    @Query("SELECT * FROM chat_message WHERE message_id = :messageId LIMIT 1")
    ChatMessageEntity getMessageByRemoteId(Long messageId);

    // 获取会话对应的所有消息：优先使用服务端 send_time 排序；发送中消息使用 client_order_time 兜底；最后用 local_id 稳定排序
    @Query("SELECT * FROM chat_message WHERE session_id = :sessionId ORDER BY (CASE WHEN send_time IS NULL OR send_time = 0 THEN client_order_time ELSE send_time END) ASC, local_id ASC")
    LiveData<List<ChatMessageEntity>> getMessagesBySessionId(String sessionId);

    // 拿到最新的一条消息（比如判断是否需要更新会话）
    @Query("SELECT * FROM chat_message WHERE session_id = :sessionId ORDER BY send_time DESC LIMIT 1")
    ChatMessageEntity getLastMessage(String sessionId);

    // 删除单个会话的所有消息
    @Query("DELETE FROM chat_message WHERE session_id = :sessionId")
    void deleteMessagesBySessionId(String sessionId);

    @Query("DELETE FROM chat_message WHERE local_id = :localId")
    void deleteByLocalId(String localId);
    
    // 清空所有消息（退出登录用）
    @Query("DELETE FROM chat_message")
    void clearAll();
}
