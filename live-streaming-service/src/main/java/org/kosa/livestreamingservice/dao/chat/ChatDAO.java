package org.kosa.livestreamingservice.dao.chat;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.kosa.livestreamingservice.dto.chat.ChatMessageDTO;

@Mapper
public interface ChatDAO {

    void insertChatMessage(ChatMessageDTO message);
    List<ChatMessageDTO> getChatMessagesByBroadcastId(Long broadcastId);
}