package com.shoppew.chat.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;

class ChatInboundSecurityInterceptorTest {

    private final JwtDecoder jwtDecoder = mock(JwtDecoder.class);
    private final JwtAuthenticationConverter authenticationConverter = mock(JwtAuthenticationConverter.class);
    private final ChatInboundSecurityInterceptor interceptor =
            new ChatInboundSecurityInterceptor(jwtDecoder, authenticationConverter);
    private final MessageChannel channel = mock(MessageChannel.class);

    @Test
    void rejectsDirectSendEvenFromAuthenticatedSocket() {
        Message<byte[]> message = message(StompCommand.SEND, "/queue/chat", true);

        assertThatThrownBy(() -> interceptor.preSend(message, channel))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("not allowed");
    }

    @Test
    void permitsOnlyThePrivateUserChatSubscription() {
        Message<byte[]> allowed = message(StompCommand.SUBSCRIBE, "/user/queue/chat", true);
        Message<byte[]> other = message(StompCommand.SUBSCRIBE, "/queue/chat", true);

        assertThat(interceptor.preSend(allowed, channel)).isSameAs(allowed);
        assertThatThrownBy(() -> interceptor.preSend(other, channel))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("destination");
    }

    @Test
    void requiresBearerCredentialOnConnect() {
        Message<byte[]> message = message(StompCommand.CONNECT, null, false);

        assertThatThrownBy(() -> interceptor.preSend(message, channel))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("bearer token");
    }

    @Test
    void attachesValidatedAuthenticationToConnectMessage() {
        Jwt jwt = Jwt.withTokenValue("validated-token")
                .header("alg", "HS256")
                .subject("user-1")
                .build();
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken("user-1", "n/a", List.of());
        when(jwtDecoder.decode("validated-token")).thenReturn(jwt);
        when(authenticationConverter.convert(jwt)).thenReturn(authentication);
        Message<byte[]> message = message(StompCommand.CONNECT, null, false, "Bearer validated-token");

        Message<?> result = interceptor.preSend(message, channel);

        StompHeaderAccessor resultAccessor =
                MessageHeaderAccessor.getAccessor(result, StompHeaderAccessor.class);
        assertThat(resultAccessor).isNotNull();
        assertThat(resultAccessor.getUser()).isSameAs(authentication);
    }

    private Message<byte[]> message(StompCommand command, String destination, boolean authenticated) {
        return message(command, destination, authenticated, null);
    }

    private Message<byte[]> message(
            StompCommand command,
            String destination,
            boolean authenticated,
            String authorization) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(command);
        if (destination != null) accessor.setDestination(destination);
        if (authorization != null) accessor.addNativeHeader("Authorization", authorization);
        if (authenticated) {
            accessor.setUser(new UsernamePasswordAuthenticationToken("user", "n/a", List.of()));
        }
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }
}
