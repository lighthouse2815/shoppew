package com.shoppew.refund.dto;

import jakarta.validation.constraints.Size;

public record RefundReviewRequest(@Size(max = 1000) String note) {}
