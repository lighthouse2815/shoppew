package com.shoppew.cart.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record CartQuantityRequest(@Min(1) @Max(999) long quantity) {}
