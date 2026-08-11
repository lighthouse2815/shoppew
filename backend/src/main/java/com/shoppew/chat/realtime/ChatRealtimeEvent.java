package com.shoppew.chat.realtime;

import com.shoppew.chat.dto.MessageResponse;
import java.util.UUID;

public record ChatRealtimeEvent(
        String event,
        UUID conversationId,
        MessageResponse message) {}
