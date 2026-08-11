package com.shoppew.payment.entity;

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
import java.util.UUID;

@Entity
@Table(name = "payment_events")
public class PaymentEventEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "payment_id", nullable = false)
    private PaymentEntity payment;
    @Column(nullable = false, length = 32)
    private String provider;
    @Column(name = "provider_event_id", nullable = false, length = 180)
    private String providerEventId;
    @Column(name = "event_type", nullable = false, length = 80)
    private String eventType;
    @Column(name = "payload_hash", nullable = false, length = 128)
    private String payloadHash;
    @Column(name = "processed_at")
    private Instant processedAt;
    @Column(name = "processing_error", length = 1000)
    private String processingError;
    @Column(name = "received_at", nullable = false, updatable = false)
    private Instant receivedAt;

    protected PaymentEventEntity() {}

    public static PaymentEventEntity receive(
            PaymentEntity payment, String provider, String eventId, String eventType,
            String payloadHash, Instant now) {
        PaymentEventEntity event = new PaymentEventEntity();
        event.payment = payment;
        event.provider = provider;
        event.providerEventId = eventId;
        event.eventType = eventType;
        event.payloadHash = payloadHash;
        event.receivedAt = now;
        return event;
    }

    public UUID getId() { return id; }
    public PaymentEntity getPayment() { return payment; }
    public String getProviderEventId() { return providerEventId; }
    public String getPayloadHash() { return payloadHash; }
    public Instant getProcessedAt() { return processedAt; }

    public void processed(Instant now) { processedAt = now; processingError = null; }
    public void failed(String message) { processingError = message; }
}
