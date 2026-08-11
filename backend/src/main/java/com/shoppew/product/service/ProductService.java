package com.shoppew.product.service;

import com.shoppew.catalog.entity.BrandEntity;
import com.shoppew.catalog.entity.CatalogStatus;
import com.shoppew.catalog.entity.CategoryEntity;
import com.shoppew.catalog.repository.BrandRepository;
import com.shoppew.catalog.repository.CategoryRepository;
import com.shoppew.common.api.PageResponse;
import com.shoppew.common.exception.ApiException;
import com.shoppew.common.text.SlugService;
import com.shoppew.product.dto.ProductDetailResponse;
import com.shoppew.product.dto.ProductRequest;
import com.shoppew.product.dto.ProductSummaryResponse;
import com.shoppew.product.entity.ProductAttributeEntity;
import com.shoppew.product.entity.ProductAttributeValueEntity;
import com.shoppew.product.entity.ProductEntity;
import com.shoppew.product.entity.ProductStatus;
import com.shoppew.product.entity.VariantStatus;
import com.shoppew.product.repository.ProductAttributeRepository;
import com.shoppew.product.repository.ProductAttributeValueRepository;
import com.shoppew.product.repository.ProductImageRepository;
import com.shoppew.product.repository.ProductRepository;
import com.shoppew.product.repository.ProductVariantRepository;
import com.shoppew.shop.entity.ShopEntity;
import com.shoppew.shop.entity.ShopStatus;
import com.shoppew.shop.service.ShopAccessService;
import java.time.Clock;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final BrandRepository brandRepository;
    private final ProductVariantRepository variantRepository;
    private final ProductImageRepository imageRepository;
    private final ProductAttributeRepository attributeRepository;
    private final ProductAttributeValueRepository attributeValueRepository;
    private final ShopAccessService shopAccessService;
    private final ProductResponseAssembler assembler;
    private final SlugService slugService;
    private final Clock clock;

    public ProductService(
            ProductRepository productRepository,
            CategoryRepository categoryRepository,
            BrandRepository brandRepository,
            ProductVariantRepository variantRepository,
            ProductImageRepository imageRepository,
            ProductAttributeRepository attributeRepository,
            ProductAttributeValueRepository attributeValueRepository,
            ShopAccessService shopAccessService,
            ProductResponseAssembler assembler,
            SlugService slugService,
            Clock clock) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.brandRepository = brandRepository;
        this.variantRepository = variantRepository;
        this.imageRepository = imageRepository;
        this.attributeRepository = attributeRepository;
        this.attributeValueRepository = attributeValueRepository;
        this.shopAccessService = shopAccessService;
        this.assembler = assembler;
        this.slugService = slugService;
        this.clock = clock;
    }

    @Transactional
    public ProductDetailResponse create(UUID userId, UUID shopId, ProductRequest request) {
        ShopEntity shop = shopAccessService.requireActiveMember(userId, shopId).getShop();
        CategoryEntity category = requireActiveCategory(request.categoryId());
        BrandEntity brand = requireActiveBrand(request.brandId());
        String slug = resolveSlug(request.slug(), shop.getSlug() + "-" + request.name());
        if (productRepository.existsBySlug(slug)) {
            throw slugConflict();
        }
        ProductEntity product = productRepository.save(ProductEntity.create(
                shop,
                category,
                brand,
                request.name().strip(),
                slug,
                trimToNull(request.shortDescription()),
                request.description().strip(),
                Instant.now(clock)));
        return assembler.detail(product);
    }

    @Transactional
    public ProductDetailResponse update(UUID userId, UUID shopId, UUID productId, ProductRequest request) {
        ProductEntity product = requireOwned(userId, shopId, productId);
        requireSellerEditable(product);
        CategoryEntity category = requireActiveCategory(request.categoryId());
        BrandEntity brand = requireActiveBrand(request.brandId());
        String slug = resolveSlug(request.slug(), product.getShop().getSlug() + "-" + request.name());
        if (productRepository.existsBySlugAndIdNot(slug, productId)) {
            throw slugConflict();
        }
        product.update(
                category,
                brand,
                request.name().strip(),
                slug,
                trimToNull(request.shortDescription()),
                request.description().strip(),
                Instant.now(clock));
        return assembler.detail(product);
    }

    @Transactional(readOnly = true)
    public ProductDetailResponse sellerDetail(UUID userId, UUID shopId, UUID productId) {
        return assembler.detail(requireOwned(userId, shopId, productId));
    }

    @Transactional(readOnly = true)
    public PageResponse<ProductSummaryResponse> sellerList(
            UUID userId,
            UUID shopId,
            ProductStatus status,
            int page,
            int size) {
        shopAccessService.requireActiveMember(userId, shopId);
        Page<ProductEntity> result = productRepository.findSellerProducts(
                shopId,
                status,
                pageRequest(page, size));
        return page(result, assembler.summaries(result.getContent()));
    }

    @Transactional(readOnly = true)
    public PageResponse<ProductSummaryResponse> publicSearch(
            String query,
            UUID shopId,
            UUID categoryId,
            UUID brandId,
            int page,
            int size) {
        Page<ProductEntity> result = productRepository.searchPublic(
                query == null ? "" : query.strip(), shopId, categoryId, brandId, pageRequest(page, size));
        return page(result, assembler.summaries(result.getContent()));
    }

    @Transactional(readOnly = true)
    public ProductDetailResponse publicDetail(String slug) {
        ProductEntity product = productRepository.findPublicBySlug(slugService.normalize(slug))
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND, "PRODUCT_NOT_FOUND", "Không tìm thấy sản phẩm"));
        return assembler.detail(product);
    }

    @Transactional
    public ProductDetailResponse submit(UUID userId, UUID shopId, UUID productId) {
        ProductEntity product = requireOwned(userId, shopId, productId);
        requireSellerEditable(product);
        validateCompleteness(product);
        product.submit(Instant.now(clock));
        return assembler.detail(product);
    }

    @Transactional
    public ProductDetailResponse archive(UUID userId, UUID shopId, UUID productId) {
        ProductEntity product = requireOwned(userId, shopId, productId);
        product.archive(Instant.now(clock));
        return assembler.detail(product);
    }

    @Transactional(readOnly = true)
    public PageResponse<ProductSummaryResponse> pendingProducts(int page, int size) {
        Page<ProductEntity> result = productRepository.findAllByStatus(
                ProductStatus.PENDING_REVIEW,
                pageRequest(page, size));
        return page(result, assembler.summaries(result.getContent()));
    }

    @Transactional
    public ProductDetailResponse approve(UUID productId) {
        ProductEntity product = requireAny(productId);
        if (product.getStatus() != ProductStatus.PENDING_REVIEW) {
            throw invalidModerationState();
        }
        if (product.getShop().getStatus() != ShopStatus.ACTIVE) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "SHOP_NOT_ACTIVE",
                    "Cửa hàng phải hoạt động trước khi duyệt sản phẩm");
        }
        validateCompleteness(product);
        product.approve(Instant.now(clock));
        return assembler.detail(product);
    }

    @Transactional
    public ProductDetailResponse reject(UUID productId, String reason) {
        ProductEntity product = requireAny(productId);
        if (product.getStatus() != ProductStatus.PENDING_REVIEW) {
            throw invalidModerationState();
        }
        String normalizedReason = requireModerationReason(reason);
        product.reject(normalizedReason, Instant.now(clock));
        return assembler.detail(product);
    }

    @Transactional
    public ProductDetailResponse hide(UUID productId, String reason) {
        ProductEntity product = requireAny(productId);
        if (product.getStatus() != ProductStatus.ACTIVE) {
            throw invalidModerationState();
        }
        product.hide(requireModerationReason(reason), Instant.now(clock));
        return assembler.detail(product);
    }

    @Transactional(readOnly = true)
    public ProductEntity requireOwned(UUID userId, UUID shopId, UUID productId) {
        shopAccessService.requireActiveMember(userId, shopId);
        return productRepository.findOwned(productId, shopId)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND, "PRODUCT_NOT_FOUND", "Không tìm thấy sản phẩm"));
    }

    public void requireSellerEditable(ProductEntity product) {
        if (!product.isSellerEditable()) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "PRODUCT_NOT_EDITABLE",
                    "Không thể chỉnh sửa sản phẩm ở trạng thái hiện tại");
        }
    }

    private ProductEntity requireAny(UUID id) {
        return productRepository.findDetailedById(id).orElseThrow(() -> new ApiException(
                HttpStatus.NOT_FOUND, "PRODUCT_NOT_FOUND", "Không tìm thấy sản phẩm"));
    }

    private CategoryEntity requireActiveCategory(UUID categoryId) {
        CategoryEntity category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "CATEGORY_NOT_FOUND", "Không tìm thấy danh mục"));
        if (category.getStatus() != CatalogStatus.ACTIVE) {
            throw new ApiException(HttpStatus.CONFLICT, "CATEGORY_NOT_ACTIVE", "Danh mục hiện không hoạt động");
        }
        return category;
    }

    private BrandEntity requireActiveBrand(UUID brandId) {
        if (brandId == null) return null;
        BrandEntity brand = brandRepository.findById(brandId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "BRAND_NOT_FOUND", "Không tìm thấy thương hiệu"));
        if (brand.getStatus() != CatalogStatus.ACTIVE) {
            throw new ApiException(HttpStatus.CONFLICT, "BRAND_NOT_ACTIVE", "Thương hiệu hiện không hoạt động");
        }
        return brand;
    }

    private void validateCompleteness(ProductEntity product) {
        if (!variantRepository.existsByProduct_IdAndStatus(product.getId(), VariantStatus.ACTIVE)) {
            throw incomplete("Sản phẩm cần ít nhất một phân loại đang hoạt động");
        }
        if (!imageRepository.existsByProduct_Id(product.getId())) {
            throw incomplete("Sản phẩm cần ít nhất một ảnh");
        }
        List<ProductAttributeEntity> required = attributeRepository.findApplicable(product.getCategory().getId())
                .stream().filter(ProductAttributeEntity::isRequired).toList();
        Set<UUID> supplied = new HashSet<>(attributeValueRepository.findAllByProduct_Id(product.getId()).stream()
                .map(ProductAttributeValueEntity::getAttribute)
                .map(ProductAttributeEntity::getId)
                .toList());
        if (required.stream().anyMatch(attribute -> !supplied.contains(attribute.getId()))) {
            throw incomplete("Sản phẩm còn thiếu thuộc tính bắt buộc");
        }
    }

    private String resolveSlug(String requested, String fallback) {
        String slug = slugService.normalize(requested == null || requested.isBlank() ? fallback : requested);
        if (slug.isBlank() || slug.length() > 280) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_PRODUCT_SLUG", "Đường dẫn sản phẩm không hợp lệ");
        }
        return slug;
    }

    private PageRequest pageRequest(int page, int size) {
        return PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100), Sort.by(Sort.Direction.DESC, "createdAt"));
    }

    private PageResponse<ProductSummaryResponse> page(Page<ProductEntity> source, List<ProductSummaryResponse> content) {
        return new PageResponse<>(content, source.getNumber(), source.getSize(), source.getTotalElements(), source.getTotalPages());
    }

    private String requireModerationReason(String reason) {
        String normalized = trimToNull(reason);
        if (normalized == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "MODERATION_REASON_REQUIRED", "Cần cung cấp lý do kiểm duyệt");
        }
        return normalized;
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }

    private ApiException slugConflict() {
        return new ApiException(HttpStatus.CONFLICT, "PRODUCT_SLUG_ALREADY_USED", "Đường dẫn sản phẩm đã được sử dụng");
    }

    private ApiException incomplete(String message) {
        return new ApiException(HttpStatus.CONFLICT, "PRODUCT_INCOMPLETE", message);
    }

    private ApiException invalidModerationState() {
        return new ApiException(HttpStatus.CONFLICT, "INVALID_PRODUCT_MODERATION_STATE", "Trạng thái sản phẩm không phù hợp");
    }
}
