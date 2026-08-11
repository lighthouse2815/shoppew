package com.shoppew.audit.service;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import com.shoppew.audit.dto.AuditLogResponse;
import com.shoppew.common.api.PageResponse;
import jakarta.servlet.http.HttpServletRequest;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Service
public class AdminAuditService {
    private final JdbcTemplate jdbc; private final ObjectMapper mapper; private final Clock clock;
    public AdminAuditService(JdbcTemplate jdbc, ObjectMapper mapper, Clock clock) { this.jdbc = jdbc; this.mapper = mapper; this.clock = clock; }

    @Transactional
    public void record(UUID actorId, String action, String resourceType, UUID resourceId, Object before, Object after) {
        actorId = actorId == null ? currentActorId() : actorId;
        HttpServletRequest request = currentRequest();
        UUID id = UUID.randomUUID(); Instant now = Instant.now(clock);
        jdbc.update("""
                insert into audit_logs(id, actor_id, action, resource_type, resource_id, before_state, after_state,
                    ip_address, user_agent, request_id, created_at)
                values (?, ?, ?, ?, ?, cast(? as jsonb), cast(? as jsonb), cast(? as inet), ?, ?, ?)
                """, id, actorId, action, resourceType, resourceId, json(before), json(after),
                request == null ? null : request.getRemoteAddr(), request == null ? null : limit(request.getHeader("User-Agent"), 1000),
                request == null ? null : limit(request.getHeader("X-Request-Id"), 128), Timestamp.from(now));
    }

    @Transactional(readOnly = true)
    public PageResponse<AuditLogResponse> list(int page, int size) {
        long total = jdbc.queryForObject("select count(*) from audit_logs", Long.class);
        List<AuditLogResponse> content = jdbc.query("""
                select id, actor_id, action, resource_type, resource_id, before_state::text, after_state::text,
                    host(ip_address), user_agent, request_id, created_at
                from audit_logs order by created_at desc limit ? offset ?
                """, (rs, row) -> new AuditLogResponse(rs.getObject("id", UUID.class), rs.getObject("actor_id", UUID.class),
                rs.getString("action"), rs.getString("resource_type"), rs.getObject("resource_id", UUID.class),
                node(rs.getString(6)), node(rs.getString(7)), rs.getString(8), rs.getString(9), rs.getString(10),
                rs.getTimestamp(11).toInstant()), size, page * size);
        return new PageResponse<>(content, page, size, total, (int) Math.ceil((double) total / size));
    }

    private HttpServletRequest currentRequest() {
        return RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes ? attributes.getRequest() : null;
    }
    private UUID currentActorId() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof JwtAuthenticationToken token) {
            try { return UUID.fromString(token.getToken().getSubject()); } catch (IllegalArgumentException ignored) { return null; }
        }
        return null;
    }
    private String json(Object value) {
        if (value == null) return null;
        try { return mapper.writeValueAsString(value); } catch (JacksonException exception) { throw new IllegalArgumentException("Could not serialize audit state", exception); }
    }
    private JsonNode node(String value) {
        if (value == null) return null;
        try { return mapper.readTree(value); } catch (JacksonException exception) { throw new IllegalStateException("Invalid stored audit JSON", exception); }
    }
    private String limit(String value, int max) { return value == null || value.length() <= max ? value : value.substring(0, max); }
}
