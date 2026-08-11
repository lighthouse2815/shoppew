package com.shoppew.chat.controller;

import com.shoppew.auth.security.AuthenticatedUser;
import com.shoppew.chat.dto.ConversationResponse;
import com.shoppew.chat.dto.MessageResponse;
import com.shoppew.chat.dto.SendMessageRequest;
import com.shoppew.chat.service.ChatService;
import com.shoppew.common.api.ApiResponse;
import com.shoppew.common.api.PageResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.time.Clock;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/seller/shops/{shopId}/chat/conversations")
public class SellerChatController {

    private final ChatService service;
    private final AuthenticatedUser user;
    private final Clock clock;

    public SellerChatController(ChatService service, AuthenticatedUser user, Clock clock) {
        this.service = service;
        this.user = user;
        this.clock = clock;
    }

    @GetMapping
    ApiResponse<PageResponse<ConversationResponse>> list(
            @PathVariable UUID shopId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return ApiResponse.success(service.sellerConversations(user.id(), shopId, page, size), clock);
    }

    @GetMapping("/{conversationId}/messages")
    ApiResponse<PageResponse<MessageResponse>> messages(
            @PathVariable UUID shopId,
            @PathVariable UUID conversationId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "50") @Min(1) @Max(100) int size) {
        return ApiResponse.success(
                service.sellerMessages(user.id(), shopId, conversationId, page, size), clock);
    }

    @PostMapping("/{conversationId}/messages")
    @ResponseStatus(HttpStatus.CREATED)
    ApiResponse<MessageResponse> send(
            @PathVariable UUID shopId,
            @PathVariable UUID conversationId,
            @Valid @RequestBody SendMessageRequest request) {
        return ApiResponse.success(
                service.sellerSend(user.id(), shopId, conversationId, request), clock);
    }
}
