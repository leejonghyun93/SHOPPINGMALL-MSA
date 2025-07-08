package org.kosa.livestreamingservice.service.chat;

import java.util.List;


import org.kosa.livestreamingservice.dao.chat.ChatDAO;
import org.kosa.livestreamingservice.dto.chat.ChatMessageDTO;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatService {

    private final ChatDAO chatDAO;

    public void saveChatMessage(ChatMessageDTO message) {
        log.info("💾 DB 저장 요청: {}", message);

        chatDAO.insertChatMessage(message);
    }

    public List<ChatMessageDTO> getHistoryByBroadcastId(Long broadcastId) {
        return chatDAO.getChatMessagesByBroadcastId(broadcastId);
    }
}