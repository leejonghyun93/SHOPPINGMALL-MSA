package org.kosa.livestreamingservice.controller.chat;

import java.util.List;

import org.kosa.livestreamingservice.dto.chat.ChatMessageDTO;
import org.kosa.livestreamingservice.service.chat.ChatService;

import org.kosa.livestreamingservice.util.ChatFilterUtil;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@Controller
@Slf4j

public class ChatController {

    private final SimpMessagingTemplate messagingTemplate;
    private final ChatService chatService;

    @MessageMapping("/sendMessage")
    public void sendMessage(ChatMessageDTO message) {

        if (message.getType() == null || message.getType().isBlank()) {
            message.setType("text");
        }

        //욕설 필터링
        message.setText(ChatFilterUtil.filterBadWords(message.getText()));

        chatService.saveChatMessage(message);

        //전체 채팅방에 뿌림
        messagingTemplate.convertAndSend("/topic/public", message);
    }

    @GetMapping("/api/chat/history/{broadcastId}")
    public ResponseEntity<List<ChatMessageDTO>> getChatHistory(@PathVariable Long broadcastId) {
        return ResponseEntity.ok(chatService.getHistoryByBroadcastId(broadcastId));
    }
}