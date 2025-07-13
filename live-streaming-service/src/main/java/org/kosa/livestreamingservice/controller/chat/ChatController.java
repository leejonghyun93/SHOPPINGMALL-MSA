package org.kosa.livestreamingservice.controller.chat;

import java.util.List;
import java.util.Map;

import org.kosa.livestreamingservice.ChatSessionManager;
import org.kosa.livestreamingservice.dto.chat.BroadcastStatusDTO;
import org.kosa.livestreamingservice.dto.chat.ChatMessageDTO;
import org.kosa.livestreamingservice.service.chat.ChatService;

import org.kosa.livestreamingservice.util.ChatFilterUtil;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@Controller
@Slf4j
public class ChatController {

    private final SimpMessagingTemplate messagingTemplate;
    private final ChatService chatService;
    private final ChatSessionManager chatSessionManager;

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

    @GetMapping("/api/broadcasts/{broadcastId}/status")
    public ResponseEntity<BroadcastStatusDTO> getBroadcastStatus(@PathVariable Long broadcastId) {
        BroadcastStatusDTO dto = chatService.getBroadcastStatus(broadcastId);
        return ResponseEntity.ok(dto);
    }

    @GetMapping("/api/chat/participants/{broadcastId}")
    public ResponseEntity<Map<String, Object>> getParticipantCount(@PathVariable Long broadcastId) {
        int count = chatSessionManager.getParticipantCount(broadcastId);
        return ResponseEntity.ok(Map.of("count", count));
    }

    @PostMapping("/api/chat/disconnect/{broadcastId}")
    @ResponseBody
    public ResponseEntity<Void> disconnectManually(
            @PathVariable Long broadcastId,
            @RequestParam("id") String id) {

        chatSessionManager.removeSessionManually(broadcastId, id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/api/chat/ban")
    public ResponseEntity<Void> banUserFromChat(
            @RequestParam Long broadcastId,
            @RequestParam String userId,
            @RequestParam(defaultValue = "300") long durationSeconds) {
        chatService.banUser(broadcastId, userId, durationSeconds);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/api/chat/ban-status/{broadcastId}/{userId}")
    public ResponseEntity<Map<String, Boolean>> checkUserBanStatus(
            @PathVariable Long broadcastId,
            @PathVariable String userId) {

        boolean banned = chatService.isUserBanned(broadcastId, userId);
        return ResponseEntity.ok(Map.of("banned", banned));
    }
}




/*
 * @MessageMapping("/sendMessage") 클라이언트가 /app/sendMessage 로 보내면 이 메서드가 받음
 * @SendTo 모든 구독자에게 메세지를 뿌릴 경로
 * return message 다시 브로커에게 전달
 *
 *
 *
 */
