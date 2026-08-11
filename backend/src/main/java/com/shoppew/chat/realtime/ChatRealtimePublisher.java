package com.shoppew.chat.realtime;

import java.util.Collection;
import java.util.UUID;

public interface ChatRealtimePublisher {

    void publish(Collection<UUID> recipientIds, ChatRealtimeEvent event);
}
