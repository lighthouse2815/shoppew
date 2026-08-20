package com.shoppew.notification.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PushDeviceRevocationRequest(
        @NotBlank @Size(min = 10, max = 512) String target) {}
