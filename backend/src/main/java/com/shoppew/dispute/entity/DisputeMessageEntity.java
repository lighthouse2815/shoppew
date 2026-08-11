package com.shoppew.dispute.entity;

import com.shoppew.user.entity.UserEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity @Table(name = "dispute_messages")
public class DisputeMessageEntity {
    @Id @GeneratedValue(strategy = GenerationType.UUID) private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "dispute_id") private DisputeEntity dispute;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "author_id") private UserEntity author;
    @Column(nullable = false, columnDefinition = "text") private String content;
    @JdbcTypeCode(SqlTypes.JSON) @Column(nullable = false, columnDefinition = "jsonb") private List<String> attachments;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
    protected DisputeMessageEntity() {}
    public static DisputeMessageEntity create(DisputeEntity dispute, UserEntity author, String content, List<String> attachments, Instant now) {
        DisputeMessageEntity message = new DisputeMessageEntity(); message.dispute = dispute; message.author = author;
        message.content = content; message.attachments = attachments; message.createdAt = now; return message;
    }
    public UUID getId() { return id; }
    public UUID getAuthorId() { return author.getId(); }
    public String getContent() { return content; }
    public List<String> getAttachments() { return List.copyOf(attachments); }
    public Instant getCreatedAt() { return createdAt; }
}
