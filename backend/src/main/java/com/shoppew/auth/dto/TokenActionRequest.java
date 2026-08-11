package com.shoppew.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TokenActionRequest(
        @NotBlank @Size(max = 512) String token) {}
