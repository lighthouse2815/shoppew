package com.shoppew.chat.dto;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record StartConversationRequest(
        @NotNull UUID shopId,
        UUID productId) {}
