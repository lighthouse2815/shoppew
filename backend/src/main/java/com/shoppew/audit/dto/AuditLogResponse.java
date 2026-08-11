package com.shoppew.audit.dto;

import tools.jackson.databind.JsonNode;
import java.time.Instant;
import java.util.UUID;

public record AuditLogResponse(UUID id, UUID actorId, String action, String resourceType, UUID resourceId,
        JsonNode before, JsonNode after, String ipAddress, String userAgent, String requestId, Instant createdAt) {}
