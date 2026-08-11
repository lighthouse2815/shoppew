package com.shoppew.review.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public record ReviewUpdateRequest(@Min(1) @Max(5) int rating, @Size(max = 5000) String content) {}
