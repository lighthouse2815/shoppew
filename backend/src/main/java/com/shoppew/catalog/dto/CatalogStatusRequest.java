package com.shoppew.catalog.dto;

import com.shoppew.catalog.entity.CatalogStatus;
import jakarta.validation.constraints.NotNull;

public record CatalogStatusRequest(@NotNull CatalogStatus status) {}
