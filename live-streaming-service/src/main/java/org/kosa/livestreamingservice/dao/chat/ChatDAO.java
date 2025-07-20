package org.kosa.livestreamingservice.dao.chat;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.kosa.livestreamingservice.dto.chat.BroadcastStatusDTO;
import org.kosa.livestreamingservice.dto.chat.ChatMessageDTO;

import org.kosa.livestreamingservice.entity.BroadcastEntity;

@Mapper
public interface ChatDAO {

    void insertChatMessage(ChatMessageDTO message);
    List<ChatMessageDTO> getChatMessagesByBroadcastId(Long broadcastId);
    String getBroadcasterIdByBroadcastId(Long broadcastId);
    BroadcastStatusDTO getBroadcastStatusById(@Param("broadcastId") Long broadcastId);
    public void updateStatus(BroadcastEntity broadCast);
}