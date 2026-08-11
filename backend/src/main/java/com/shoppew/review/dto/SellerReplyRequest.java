package com.shoppew.review.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SellerReplyRequest(@NotBlank @Size(max = 5000) String reply) {}
