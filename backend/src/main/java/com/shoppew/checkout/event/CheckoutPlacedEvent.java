package com.shoppew.checkout.event;

import java.util.UUID;

public record CheckoutPlacedEvent(UUID checkoutId) {}
