package com.shoppew.dispute.dto;

import com.shoppew.common.validation.HttpUrl;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;

public record DisputeMessageRequest(@NotBlank @Size(max = 5000) String content,
        @Size(max = 5) List<@Size(max = 1000) @HttpUrl String> attachments) {}
