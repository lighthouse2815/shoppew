package com.shoppew.catalog.service;

import com.shoppew.catalog.dto.BrandRequest;
import com.shoppew.catalog.dto.BrandResponse;
import com.shoppew.catalog.entity.BrandEntity;
import com.shoppew.catalog.entity.CatalogStatus;
import com.shoppew.catalog.repository.BrandRepository;
import com.shoppew.common.exception.ApiException;
import com.shoppew.common.text.SlugService;
import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BrandService {

    private final BrandRepository repository;
    private final SlugService slugService;
    private final Clock clock;

    public BrandService(BrandRepository repository, SlugService slugService, Clock clock) {
        this.repository = repository;
        this.slugService = slugService;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<BrandResponse> publicList() {
        return repository.findAllByStatusOrderByNameAsc(CatalogStatus.ACTIVE).stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<BrandResponse> adminList() {
        return repository.findAllByOrderByNameAsc().stream().map(this::toResponse).toList();
    }

    @Transactional
    public BrandResponse create(BrandRequest request) {
        String name = request.name().strip();
        String slug = resolveSlug(request.slug(), name);
        if (repository.existsByNameIgnoreCase(name)) {
            throw new ApiException(HttpStatus.CONFLICT, "BRAND_NAME_ALREADY_USED", "Tên thương hiệu đã được sử dụng");
        }
        if (repository.existsBySlug(slug)) {
            throw conflictSlug();
        }
        return toResponse(repository.save(BrandEntity.create(
                name, slug, validateOptionalUrl(request.logoUrl()), Instant.now(clock))));
    }

    @Transactional
    public BrandResponse update(UUID brandId, BrandRequest request) {
        BrandEntity brand = require(brandId);
        String name = request.name().strip();
        String slug = resolveSlug(request.slug(), name);
        if (repository.existsByNameIgnoreCaseAndIdNot(name, brandId)) {
            throw new ApiException(HttpStatus.CONFLICT, "BRAND_NAME_ALREADY_USED", "Tên thương hiệu đã được sử dụng");
        }
        if (repository.existsBySlugAndIdNot(slug, brandId)) {
            throw conflictSlug();
        }
        brand.update(name, slug, validateOptionalUrl(request.logoUrl()), Instant.now(clock));
        return toResponse(brand);
    }

    @Transactional
    public BrandResponse changeStatus(UUID brandId, CatalogStatus status) {
        BrandEntity brand = require(brandId);
        brand.changeStatus(status, Instant.now(clock));
        return toResponse(brand);
    }

    private BrandEntity require(UUID id) {
        return repository.findById(id).orElseThrow(() -> new ApiException(
                HttpStatus.NOT_FOUND, "BRAND_NOT_FOUND", "Không tìm thấy thương hiệu"));
    }

    private String resolveSlug(String requested, String name) {
        String slug = slugService.normalize(requested == null || requested.isBlank() ? name : requested);
        if (slug.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_BRAND_SLUG", "Đường dẫn thương hiệu không hợp lệ");
        }
        return slug;
    }

    private String validateOptionalUrl(String value) {
        String normalized = value == null || value.isBlank() ? null : value.strip();
        if (normalized == null) return null;
        try {
            URI uri = URI.create(normalized);
            if (!"http".equalsIgnoreCase(uri.getScheme()) && !"https".equalsIgnoreCase(uri.getScheme())) {
                throw new IllegalArgumentException();
            }
            return normalized;
        } catch (IllegalArgumentException exception) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_LOGO_URL", "URL logo không hợp lệ");
        }
    }

    private BrandResponse toResponse(BrandEntity brand) {
        return new BrandResponse(
                brand.getId(), brand.getName(), brand.getSlug(), brand.getLogoUrl(), brand.getStatus().name(),
                brand.getCreatedAt(), brand.getUpdatedAt());
    }

    private ApiException conflictSlug() {
        return new ApiException(HttpStatus.CONFLICT, "BRAND_SLUG_ALREADY_USED", "Đường dẫn thương hiệu đã được sử dụng");
    }
}
