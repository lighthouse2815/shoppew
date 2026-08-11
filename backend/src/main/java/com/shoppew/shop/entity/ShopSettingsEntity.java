package com.shoppew.shop.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "shop_settings")
public class ShopSettingsEntity {

    @Id
    @Column(name = "shop_id")
    private UUID shopId;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "shop_id")
    private ShopEntity shop;

    @Column(name = "currency_code", nullable = false, columnDefinition = "char(3)")
    @JdbcTypeCode(SqlTypes.CHAR)
    private String currencyCode;

    @Column(name = "time_zone", nullable = false, length = 64)
    private String timeZone;

    @Column(name = "order_auto_cancel_minutes", nullable = false)
    private int orderAutoCancelMinutes;

    @Column(name = "return_window_days", nullable = false)
    private int returnWindowDays;

    @Column(name = "chat_enabled", nullable = false)
    private boolean chatEnabled;

    @Column(name = "vacation_mode", nullable = false)
    private boolean vacationMode;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected ShopSettingsEntity() {}

    public static ShopSettingsEntity defaults(ShopEntity shop, String currency, String timeZone, Instant now) {
        ShopSettingsEntity settings = new ShopSettingsEntity();
        settings.shop = shop;
        settings.currencyCode = currency;
        settings.timeZone = timeZone;
        settings.orderAutoCancelMinutes = 1440;
        settings.returnWindowDays = 7;
        settings.chatEnabled = true;
        settings.vacationMode = false;
        settings.createdAt = now;
        settings.updatedAt = now;
        return settings;
    }

    public UUID getShopId() {
        return shopId;
    }

    public String getCurrencyCode() {
        return currencyCode;
    }

    public String getTimeZone() {
        return timeZone;
    }

    public int getOrderAutoCancelMinutes() {
        return orderAutoCancelMinutes;
    }

    public int getReturnWindowDays() {
        return returnWindowDays;
    }

    public boolean isChatEnabled() {
        return chatEnabled;
    }

    public boolean isVacationMode() {
        return vacationMode;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void update(
            String currencyCode,
            String timeZone,
            int orderAutoCancelMinutes,
            int returnWindowDays,
            boolean chatEnabled,
            boolean vacationMode,
            Instant now) {
        this.currencyCode = currencyCode;
        this.timeZone = timeZone;
        this.orderAutoCancelMinutes = orderAutoCancelMinutes;
        this.returnWindowDays = returnWindowDays;
        this.chatEnabled = chatEnabled;
        this.vacationMode = vacationMode;
        this.updatedAt = now;
    }
}
