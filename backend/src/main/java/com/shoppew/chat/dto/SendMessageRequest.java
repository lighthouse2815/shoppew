package com.shoppew.chat.dto;

import com.shoppew.common.validation.HttpUrl;
import com.shoppew.chat.entity.MessageType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record SendMessageRequest(
        @NotNull MessageType type,
        @Size(max = 4000) String textContent,
        @Size(max = 1000) @HttpUrl String mediaUrl,
        UUID productId,
        UUID orderId) {}
