package com.shoppew.chat.realtime;

import java.util.Collection;
import java.util.UUID;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
public class WebSocketChatRealtimePublisher implements ChatRealtimePublisher {

    private final SimpMessagingTemplate messagingTemplate;

    public WebSocketChatRealtimePublisher(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    @Override
    public void publish(Collection<UUID> recipientIds, ChatRealtimeEvent event) {
        recipientIds.stream().distinct().forEach(recipientId -> messagingTemplate.convertAndSendToUser(
                recipientId.toString(), "/queue/chat", event));
    }
}
