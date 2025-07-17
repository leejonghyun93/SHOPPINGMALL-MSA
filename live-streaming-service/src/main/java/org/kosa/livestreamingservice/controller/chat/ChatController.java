package org.kosa.livestreamingservice.controller.chat;

import java.util.List;
import java.util.Map;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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

@Tag(name = "채팅 API", description = "실시간 채팅 및 방송 관련 API")
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

    @Operation(summary = "채팅 히스토리 조회", description = "특정 방송의 채팅 히스토리를 조회합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = ChatMessageDTO.class)))
    })
    @GetMapping("/api/chat/history/{broadcastId}")
    public ResponseEntity<List<ChatMessageDTO>> getChatHistory(
            @Parameter(description = "방송 ID", required = true, example = "1")
            @PathVariable Long broadcastId) {
        return ResponseEntity.ok(chatService.getHistoryByBroadcastId(broadcastId));
    }

    @Operation(summary = "방송 상태 조회", description = "특정 방송의 상태 정보를 조회합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = BroadcastStatusDTO.class)))
    })
    @GetMapping("/api/broadcasts/{broadcastId}/status")
    public ResponseEntity<BroadcastStatusDTO> getBroadcastStatus(
            @Parameter(description = "방송 ID", required = true, example = "1")
            @PathVariable Long broadcastId) {
        BroadcastStatusDTO dto = chatService.getBroadcastStatus(broadcastId);
        return ResponseEntity.ok(dto);
    }

    @Operation(summary = "채팅 참가자 수 조회", description = "특정 방송의 채팅 참가자 수를 조회합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공")
    })
    @GetMapping("/api/chat/participants/{broadcastId}")
    public ResponseEntity<Map<String, Object>> getParticipantCount(
            @Parameter(description = "방송 ID", required = true, example = "1")
            @PathVariable Long broadcastId) {
        int count = chatSessionManager.getParticipantCount(broadcastId);
        return ResponseEntity.ok(Map.of("count", count));
    }

    @Operation(summary = "채팅 수동 연결 해제", description = "특정 사용자의 채팅 연결을 수동으로 해제합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "연결 해제 성공")
    })
    @PostMapping("/api/chat/disconnect/{broadcastId}")
    @ResponseBody
    public ResponseEntity<Void> disconnectManually(
            @Parameter(description = "방송 ID", required = true, example = "1")
            @PathVariable Long broadcastId,
            @Parameter(description = "사용자 ID", required = true, example = "user123")
            @RequestParam("id") String id) {

        chatSessionManager.removeSessionManually(broadcastId, id);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "사용자 채팅 금지", description = "특정 사용자를 일정 시간 동안 채팅에서 금지합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "금지 처리 성공")
    })
    @PostMapping("/api/chat/ban")
    public ResponseEntity<Void> banUserFromChat(
            @Parameter(description = "방송 ID", required = true, example = "1")
            @RequestParam Long broadcastId,
            @Parameter(description = "사용자 ID", required = true, example = "user123")
            @RequestParam String userId,
            @Parameter(description = "금지 시간(초)", example = "300")
            @RequestParam(defaultValue = "300") long durationSeconds) {
        chatService.banUser(broadcastId, userId, durationSeconds);
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "사용자 채팅 금지 상태 확인", description = "특정 사용자의 채팅 금지 상태를 확인합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공")
    })
    @GetMapping("/api/chat/ban-status/{broadcastId}/{userId}")
    public ResponseEntity<Map<String, Boolean>> checkUserBanStatus(
            @Parameter(description = "방송 ID", required = true, example = "1")
            @PathVariable Long broadcastId,
            @Parameter(description = "사용자 ID", required = true, example = "user123")
            @PathVariable String userId) {

        boolean banned = chatService.isUserBanned(broadcastId, userId);
        return ResponseEntity.ok(Map.of("banned", banned));
    }
}