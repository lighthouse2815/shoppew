package com.shoppew.search;

import com.shoppew.common.api.PageResponse;
import com.shoppew.common.exception.ApiException;
import com.shoppew.product.dto.ProductSummaryResponse;
import com.shoppew.product.entity.ProductEntity;
import com.shoppew.product.repository.ProductRepository;
import com.shoppew.product.service.ProductResponseAssembler;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PostgresSearchService implements SearchService {

    private static final String FROM_AND_BASE_FILTERS = """
            from products p
            join shops s on s.id = p.shop_id and s.status = 'ACTIVE'
            join lateral (
                select min(v.price) as minimum_price
                from product_variants v
                where v.product_id = p.id and v.status = 'ACTIVE'
            ) price on true
            where p.status = 'ACTIVE'
              and price.minimum_price is not null
            """;

    private final EntityManager entityManager;
    private final ProductRepository productRepository;
    private final ProductResponseAssembler assembler;

    public PostgresSearchService(
            EntityManager entityManager,
            ProductRepository productRepository,
            ProductResponseAssembler assembler) {
        this.entityManager = entityManager;
        this.productRepository = productRepository;
        this.assembler = assembler;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<ProductSummaryResponse> search(ProductSearchCriteria criteria) {
        validate(criteria);
        String normalizedQuery = normalize(criteria.query());
        SearchSort sort = criteria.sort() == null ? SearchSort.RELEVANCE : criteria.sort();
        int page = Math.max(criteria.page(), 0);
        int size = Math.min(Math.max(criteria.size(), 1), 100);

        StringBuilder filters = new StringBuilder(FROM_AND_BASE_FILTERS);
        Map<String, Object> parameters = new LinkedHashMap<>();
        if (!normalizedQuery.isEmpty()) {
            filters.append("""
                    and (
                        to_tsvector('simple', coalesce(p.name, '') || ' ' || coalesce(p.short_description, '') || ' ' || coalesce(p.description, ''))
                            @@ websearch_to_tsquery('simple', :searchQuery)
                        or p.name ilike :containsQuery
                    )
                    """);
            parameters.put("searchQuery", normalizedQuery);
            parameters.put("containsQuery", "%" + normalizedQuery + "%");
        }
        appendEquals(filters, parameters, "p.shop_id", "shopId", criteria.shopId());
        appendEquals(filters, parameters, "p.category_id", "categoryId", criteria.categoryId());
        appendEquals(filters, parameters, "p.brand_id", "brandId", criteria.brandId());
        if (criteria.minPrice() != null) {
            filters.append(" and price.minimum_price >= :minPrice\n");
            parameters.put("minPrice", criteria.minPrice());
        }
        if (criteria.maxPrice() != null) {
            filters.append(" and price.minimum_price <= :maxPrice\n");
            parameters.put("maxPrice", criteria.maxPrice());
        }
        if (criteria.minRating() != null) {
            filters.append(" and p.rating_average >= :minRating\n");
            parameters.put("minRating", criteria.minRating());
        }

        String select = "select p.id " + filters + orderBy(sort, !normalizedQuery.isEmpty());
        Query pageQuery = entityManager.createNativeQuery(select);
        bind(pageQuery, parameters);
        pageQuery.setFirstResult(page * size);
        pageQuery.setMaxResults(size);
        @SuppressWarnings("unchecked")
        List<Object> rawIds = pageQuery.getResultList();
        List<UUID> ids = rawIds.stream().map(this::uuid).toList();

        Query countQuery = entityManager.createNativeQuery("select count(*) " + filters);
        bind(countQuery, parameters);
        long total = ((Number) countQuery.getSingleResult()).longValue();
        List<ProductEntity> orderedProducts = orderedProducts(ids);
        int totalPages = total == 0 ? 0 : (int) Math.ceil((double) total / size);
        return new PageResponse<>(assembler.summaries(orderedProducts), page, size, total, totalPages);
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> suggest(String query, int size) {
        String normalized = normalize(query);
        if (normalized.length() < 2) {
            return List.of();
        }
        Query suggestionQuery = entityManager.createNativeQuery("""
                select p.name
                from products p
                join shops s on s.id = p.shop_id and s.status = 'ACTIVE'
                where p.status = 'ACTIVE'
                  and p.name ilike :prefix
                order by
                  case when lower(p.name) like lower(:startsWith) then 0 else 1 end,
                  p.sold_count desc,
                  p.rating_average desc,
                  p.name asc
                """);
        suggestionQuery.setParameter("prefix", "%" + normalized + "%");
        suggestionQuery.setParameter("startsWith", normalized + "%");
        suggestionQuery.setMaxResults(Math.min(Math.max(size, 1), 10));
        @SuppressWarnings("unchecked")
        List<Object> rows = suggestionQuery.getResultList();
        return rows.stream().map(String::valueOf).distinct().toList();
    }

    private List<ProductEntity> orderedProducts(List<UUID> ids) {
        if (ids.isEmpty()) {
            return List.of();
        }
        Map<UUID, ProductEntity> byId = productRepository.findAllDetailedByIdIn(ids).stream()
                .collect(Collectors.toMap(ProductEntity::getId, Function.identity()));
        List<ProductEntity> ordered = new ArrayList<>(ids.size());
        for (UUID id : ids) {
            ProductEntity product = byId.get(id);
            if (product != null) {
                ordered.add(product);
            }
        }
        return ordered;
    }

    private String orderBy(SearchSort sort, boolean hasQuery) {
        return switch (sort) {
            case NEWEST -> " order by p.published_at desc nulls last, p.created_at desc, p.id";
            case PRICE_ASC -> " order by price.minimum_price asc, p.id";
            case PRICE_DESC -> " order by price.minimum_price desc, p.id";
            case BEST_SELLING -> " order by p.sold_count desc, p.rating_average desc, p.id";
            case RATING -> " order by p.rating_average desc, p.review_count desc, p.sold_count desc, p.id";
            case RELEVANCE -> hasQuery
                    ? """
                       order by ts_rank_cd(
                           to_tsvector('simple', coalesce(p.name, '') || ' ' || coalesce(p.short_description, '') || ' ' || coalesce(p.description, '')),
                           websearch_to_tsquery('simple', :searchQuery)
                       ) desc, p.sold_count desc, p.rating_average desc, p.id
                       """
                    : " order by p.sold_count desc, p.rating_average desc, p.created_at desc, p.id";
        };
    }

    private void validate(ProductSearchCriteria criteria) {
        if (criteria.minPrice() != null && criteria.minPrice().signum() < 0
                || criteria.maxPrice() != null && criteria.maxPrice().signum() < 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_PRICE_RANGE", "Khoảng giá không hợp lệ");
        }
        if (criteria.minPrice() != null && criteria.maxPrice() != null
                && criteria.minPrice().compareTo(criteria.maxPrice()) > 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_PRICE_RANGE", "Giá tối thiểu không thể lớn hơn giá tối đa");
        }
        BigDecimal rating = criteria.minRating();
        if (rating != null && (rating.signum() < 0 || rating.compareTo(BigDecimal.valueOf(5)) > 0)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_RATING", "Điểm đánh giá phải từ 0 đến 5");
        }
    }

    private void appendEquals(
            StringBuilder sql,
            Map<String, Object> parameters,
            String column,
            String parameter,
            Object value) {
        if (value != null) {
            sql.append(" and ").append(column).append(" = :").append(parameter).append('\n');
            parameters.put(parameter, value);
        }
    }

    private void bind(Query query, Map<String, Object> parameters) {
        parameters.forEach(query::setParameter);
    }

    private UUID uuid(Object value) {
        return value instanceof UUID id ? id : UUID.fromString(String.valueOf(value));
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return value.strip().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }
}
