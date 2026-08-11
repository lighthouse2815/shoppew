package com.shoppew.product.dto;

import jakarta.validation.constraints.Size;

public record ProductModerationRequest(@Size(max = 1000) String reason) {}
