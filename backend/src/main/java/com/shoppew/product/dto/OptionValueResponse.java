package com.shoppew.product.dto;

import java.util.UUID;

public record OptionValueResponse(UUID id, String value, int sortOrder) {}
