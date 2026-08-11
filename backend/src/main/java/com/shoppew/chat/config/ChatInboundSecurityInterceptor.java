package com.shoppew.chat.config;

import java.util.List;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class ChatInboundSecurityInterceptor implements ChannelInterceptor {

    private static final String CHAT_DESTINATION = "/user/queue/chat";

    private final JwtDecoder jwtDecoder;
    private final JwtAuthenticationConverter authenticationConverter;

    public ChatInboundSecurityInterceptor(
            JwtDecoder jwtDecoder,
            JwtAuthenticationConverter authenticationConverter) {
        this.jwtDecoder = jwtDecoder;
        this.authenticationConverter = authenticationConverter;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);
        StompCommand command = accessor.getCommand();
        Message<?> result = message;
        if (StompCommand.CONNECT.equals(command)) {
            accessor.setUser(authentication(accessor));
            result = MessageBuilder.createMessage(message.getPayload(), accessor.getMessageHeaders());
        } else if (requiresAuthentication(command) && accessor.getUser() == null) {
            throw new AccessDeniedException("WebSocket authentication required");
        }

        if (StompCommand.SEND.equals(command)) {
            // Chat mutations go through REST so ownership and message content are validated once.
            throw new AccessDeniedException("Direct WebSocket sends are not allowed");
        }
        if (StompCommand.SUBSCRIBE.equals(command)
                && !CHAT_DESTINATION.equals(accessor.getDestination())) {
            throw new AccessDeniedException("Subscription destination is not allowed");
        }
        return result;
    }

    private boolean requiresAuthentication(StompCommand command) {
        return StompCommand.SUBSCRIBE.equals(command)
                || StompCommand.UNSUBSCRIBE.equals(command)
                || StompCommand.SEND.equals(command)
                || StompCommand.ACK.equals(command)
                || StompCommand.NACK.equals(command);
    }

    private org.springframework.security.core.Authentication authentication(StompHeaderAccessor accessor) {
        List<String> values = accessor.getNativeHeader("Authorization");
        String header = values == null || values.isEmpty() ? null : values.getFirst();
        if (!StringUtils.hasText(header) || !header.startsWith("Bearer ")) {
            throw new AccessDeniedException("WebSocket bearer token required");
        }
        Jwt jwt = jwtDecoder.decode(header.substring(7));
        return authenticationConverter.convert(jwt);
    }
}
