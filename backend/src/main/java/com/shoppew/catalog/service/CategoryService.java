package com.shoppew.catalog.service;

import com.shoppew.catalog.dto.CategoryRequest;
import com.shoppew.catalog.dto.CategoryResponse;
import com.shoppew.catalog.dto.CategoryTreeResponse;
import com.shoppew.catalog.entity.CatalogStatus;
import com.shoppew.catalog.entity.CategoryEntity;
import com.shoppew.catalog.repository.CategoryRepository;
import com.shoppew.common.exception.ApiException;
import com.shoppew.common.text.SlugService;
import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CategoryService {

    private final CategoryRepository repository;
    private final SlugService slugService;
    private final Clock clock;

    public CategoryService(CategoryRepository repository, SlugService slugService, Clock clock) {
        this.repository = repository;
        this.slugService = slugService;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<CategoryResponse> adminList() {
        return repository.findAllByOrderBySortOrderAscNameAsc().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<CategoryTreeResponse> publicTree() {
        List<CategoryEntity> active = repository.findAllByStatusOrderBySortOrderAscNameAsc(CatalogStatus.ACTIVE);
        Map<UUID, MutableNode> nodes = new HashMap<>();
        active.forEach(category -> nodes.put(category.getId(), new MutableNode(category)));
        List<MutableNode> roots = new ArrayList<>();
        for (CategoryEntity category : active) {
            MutableNode node = nodes.get(category.getId());
            if (category.getParentId() == null) {
                roots.add(node);
            } else {
                MutableNode parent = nodes.get(category.getParentId());
                if (parent != null) {
                    parent.children.add(node);
                }
            }
        }
        return roots.stream().map(MutableNode::freeze).toList();
    }

    @Transactional
    public CategoryResponse create(CategoryRequest request) {
        String slug = resolveSlug(request.slug(), request.name());
        if (repository.existsBySlug(slug)) {
            throw conflictSlug();
        }
        CategoryEntity parent = findParent(request.parentId());
        Instant now = Instant.now(clock);
        CategoryEntity category = CategoryEntity.create(
                parent,
                request.name().strip(),
                slug,
                trimToNull(request.description()),
                validateOptionalUrl(request.imageUrl()),
                request.sortOrder(),
                now);
        return toResponse(repository.save(category));
    }

    @Transactional
    public CategoryResponse update(UUID categoryId, CategoryRequest request) {
        CategoryEntity category = require(categoryId);
        String slug = resolveSlug(request.slug(), request.name());
        if (repository.existsBySlugAndIdNot(slug, categoryId)) {
            throw conflictSlug();
        }
        CategoryEntity parent = findParent(request.parentId());
        ensureNoCycle(categoryId, parent);
        category.update(
                parent,
                request.name().strip(),
                slug,
                trimToNull(request.description()),
                validateOptionalUrl(request.imageUrl()),
                request.sortOrder(),
                Instant.now(clock));
        return toResponse(category);
    }

    @Transactional
    public CategoryResponse changeStatus(UUID categoryId, CatalogStatus status) {
        CategoryEntity category = require(categoryId);
        category.changeStatus(status, Instant.now(clock));
        return toResponse(category);
    }

    private void ensureNoCycle(UUID categoryId, CategoryEntity parent) {
        Set<UUID> visited = new HashSet<>();
        CategoryEntity cursor = parent;
        while (cursor != null) {
            if (cursor.getId().equals(categoryId) || !visited.add(cursor.getId())) {
                throw new ApiException(
                        HttpStatus.BAD_REQUEST,
                        "CATEGORY_HIERARCHY_CYCLE",
                        "Danh mục cha tạo thành vòng lặp không hợp lệ");
            }
            cursor = cursor.getParent();
        }
    }

    private CategoryEntity findParent(UUID parentId) {
        return parentId == null ? null : require(parentId);
    }

    private CategoryEntity require(UUID id) {
        return repository.findById(id).orElseThrow(() -> new ApiException(
                HttpStatus.NOT_FOUND,
                "CATEGORY_NOT_FOUND",
                "Không tìm thấy danh mục"));
    }

    private String resolveSlug(String requested, String name) {
        String slug = slugService.normalize(requested == null || requested.isBlank() ? name : requested);
        if (slug.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_CATEGORY_SLUG", "Đường dẫn danh mục không hợp lệ");
        }
        return slug;
    }

    private String validateOptionalUrl(String value) {
        String normalized = trimToNull(value);
        if (normalized == null) {
            return null;
        }
        try {
            URI uri = URI.create(normalized);
            if (!"http".equalsIgnoreCase(uri.getScheme()) && !"https".equalsIgnoreCase(uri.getScheme())) {
                throw new IllegalArgumentException();
            }
            return normalized;
        } catch (IllegalArgumentException exception) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_IMAGE_URL", "URL ảnh không hợp lệ");
        }
    }

    private CategoryResponse toResponse(CategoryEntity category) {
        return new CategoryResponse(
                category.getId(), category.getParentId(), category.getName(), category.getSlug(),
                category.getDescription(), category.getImageUrl(), category.getSortOrder(),
                category.getStatus().name(), category.getCreatedAt(), category.getUpdatedAt());
    }

    private ApiException conflictSlug() {
        return new ApiException(HttpStatus.CONFLICT, "CATEGORY_SLUG_ALREADY_USED", "Đường dẫn danh mục đã được sử dụng");
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }

    private static final class MutableNode {
        private final CategoryEntity category;
        private final List<MutableNode> children = new ArrayList<>();

        private MutableNode(CategoryEntity category) {
            this.category = category;
        }

        private CategoryTreeResponse freeze() {
            return new CategoryTreeResponse(
                    category.getId(), category.getName(), category.getSlug(), category.getImageUrl(),
                    children.stream().map(MutableNode::freeze).toList());
        }
    }
}
