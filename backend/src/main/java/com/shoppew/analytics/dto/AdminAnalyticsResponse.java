package com.shoppew.analytics.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record AdminAnalyticsResponse(Instant from, Instant to, BigDecimal gmv, long completedOrders,
        long newUsers, long activeShops, long pendingModeration, BigDecimal refundVolume) {}
