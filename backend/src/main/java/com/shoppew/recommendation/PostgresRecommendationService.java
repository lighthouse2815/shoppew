package com.shoppew.recommendation;

import com.shoppew.common.exception.ApiException;
import com.shoppew.product.dto.ProductSummaryResponse;
import com.shoppew.product.entity.ProductEntity;
import com.shoppew.product.entity.ProductStatus;
import com.shoppew.product.repository.ProductRepository;
import com.shoppew.product.service.ProductResponseAssembler;
import com.shoppew.search.ProductSearchCriteria;
import com.shoppew.search.SearchService;
import com.shoppew.search.SearchSort;
import com.shoppew.shop.entity.ShopStatus;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PostgresRecommendationService implements RecommendationService {

    private final SearchService searchService;
    private final ProductRepository productRepository;
    private final ProductResponseAssembler assembler;
    private final EntityManager entityManager;

    public PostgresRecommendationService(
            SearchService searchService,
            ProductRepository productRepository,
            ProductResponseAssembler assembler,
            EntityManager entityManager) {
        this.searchService = searchService;
        this.productRepository = productRepository;
        this.assembler = assembler;
        this.entityManager = entityManager;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductSummaryResponse> popular(int size) {
        return searchService.search(criteria(null, null, SearchSort.BEST_SELLING, size)).content();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductSummaryResponse> trending(int size) {
        Query query = entityManager.createNativeQuery("""
                select p.id
                from products p
                join shops s on s.id = p.shop_id and s.status = 'ACTIVE'
                join product_variants v on v.product_id = p.id and v.status = 'ACTIVE'
                left join order_items oi on oi.product_id = p.id
                left join orders o on o.id = oi.order_id
                    and o.placed_at >= now() - interval '7 days'
                    and o.status not in ('CANCELLED', 'REFUNDED')
                where p.status = 'ACTIVE'
                group by p.id
                order by coalesce(sum(case when o.id is not null then oi.quantity else 0 end), 0) desc,
                         p.sold_count desc,
                         p.rating_average desc,
                         p.id
                """);
        query.setMaxResults(limit(size));
        return summaries(ids(query.getResultList()));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductSummaryResponse> related(UUID productId, int size) {
        ProductEntity source = requirePublic(productId);
        int limit = limit(size);
        LinkedHashMap<UUID, ProductSummaryResponse> result = new LinkedHashMap<>();
        addAllExcluding(result, searchService.search(new ProductSearchCriteria(
                null, null, source.getCategory().getId(), null, null, null, null,
                SearchSort.BEST_SELLING, 0, Math.min(limit + 1, 100))).content(), productId, limit);
        if (result.size() < limit) {
            addAllExcluding(result, sameShop(source.getShopId(), productId, limit), productId, limit);
        }
        if (result.size() < limit) {
            addAllExcluding(result, trending(limit), productId, limit);
        }
        return new ArrayList<>(result.values());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductSummaryResponse> sameShop(UUID shopId, UUID excludeProductId, int size) {
        List<ProductSummaryResponse> products = searchService.search(criteria(shopId, null, SearchSort.NEWEST, size + 1)).content();
        return products.stream()
                .filter(product -> excludeProductId == null || !product.id().equals(excludeProductId))
                .limit(limit(size))
                .toList();
    }

    @Override
    @Transactional
    public void recordView(UUID userId, UUID productId) {
        requirePublic(productId);
        entityManager.createNativeQuery("""
                insert into product_views (user_id, product_id, view_count, first_viewed_at, last_viewed_at)
                values (:userId, :productId, 1, now(), now())
                on conflict (user_id, product_id)
                do update set view_count = product_views.view_count + 1, last_viewed_at = excluded.last_viewed_at
                """)
                .setParameter("userId", userId)
                .setParameter("productId", productId)
                .executeUpdate();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProductSummaryResponse> recentlyViewed(UUID userId, int size) {
        Query query = entityManager.createNativeQuery("""
                select p.id
                from product_views view
                join products p on p.id = view.product_id and p.status = 'ACTIVE'
                join shops s on s.id = p.shop_id and s.status = 'ACTIVE'
                where view.user_id = :userId
                order by view.last_viewed_at desc, p.id
                """);
        query.setParameter("userId", userId);
        query.setMaxResults(limit(size));
        return summaries(ids(query.getResultList()));
    }

    private ProductEntity requirePublic(UUID productId) {
        ProductEntity product = productRepository.findDetailedById(productId)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND, "PRODUCT_NOT_FOUND", "Không tìm thấy sản phẩm"));
        if (product.getStatus() != ProductStatus.ACTIVE || product.getShop().getStatus() != ShopStatus.ACTIVE) {
            throw new ApiException(HttpStatus.NOT_FOUND, "PRODUCT_NOT_FOUND", "Không tìm thấy sản phẩm");
        }
        return product;
    }

    private ProductSearchCriteria criteria(UUID shopId, UUID categoryId, SearchSort sort, int size) {
        return new ProductSearchCriteria(
                null, shopId, categoryId, null, null, null, null, sort, 0, limit(size));
    }

    private List<ProductSummaryResponse> summaries(List<UUID> ids) {
        if (ids.isEmpty()) {
            return List.of();
        }
        Map<UUID, ProductEntity> byId = productRepository.findAllDetailedByIdIn(ids).stream()
                .collect(Collectors.toMap(ProductEntity::getId, Function.identity()));
        return assembler.summaries(ids.stream().map(byId::get).filter(java.util.Objects::nonNull).toList());
    }

    private List<UUID> ids(List<?> values) {
        return values.stream()
                .map(value -> value instanceof UUID id ? id : UUID.fromString(String.valueOf(value)))
                .toList();
    }

    private void addAllExcluding(
            LinkedHashMap<UUID, ProductSummaryResponse> target,
            List<ProductSummaryResponse> candidates,
            UUID excluded,
            int limit) {
        for (ProductSummaryResponse candidate : candidates) {
            if (!candidate.id().equals(excluded)) {
                target.putIfAbsent(candidate.id(), candidate);
            }
            if (target.size() >= limit) {
                return;
            }
        }
    }

    private int limit(int requested) {
        return Math.min(Math.max(requested, 1), 40);
    }
}
