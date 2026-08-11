package com.shoppew.common.system;

import java.util.List;

public record AdminSettingsResponse(
        String locale,
        String currency,
        String timeZone,
        List<String> availablePaymentProviders,
        List<String> availableShippingProviders,
        String objectStorageProvider,
        long maxUploadBytes) {}
