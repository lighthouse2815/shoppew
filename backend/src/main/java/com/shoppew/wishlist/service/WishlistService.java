package com.shoppew.wishlist.service;

import com.shoppew.common.exception.ApiException;
import com.shoppew.product.entity.ProductEntity;
import com.shoppew.product.entity.ProductStatus;
import com.shoppew.product.repository.ProductRepository;
import com.shoppew.product.service.ProductResponseAssembler;
import com.shoppew.user.entity.UserEntity;
import com.shoppew.user.repository.UserRepository;
import com.shoppew.wishlist.dto.WishlistResponse;
import com.shoppew.wishlist.entity.WishlistEntity;
import com.shoppew.wishlist.repository.WishlistRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class WishlistService {
    private final WishlistRepository repository; private final ProductRepository productRepository;
    private final UserRepository userRepository; private final ProductResponseAssembler assembler; private final Clock clock;
    public WishlistService(WishlistRepository repository, ProductRepository productRepository,
            UserRepository userRepository, ProductResponseAssembler assembler, Clock clock) {
        this.repository = repository; this.productRepository = productRepository;
        this.userRepository = userRepository; this.assembler = assembler; this.clock = clock;
    }
    @Transactional(readOnly = true)
    public List<WishlistResponse> list(UUID userId) {
        List<WishlistEntity> items = repository.findAllByUser_IdOrderByCreatedAtDesc(userId);
        var summaries = assembler.summaries(items.stream().map(WishlistEntity::getProduct).toList());
        return java.util.stream.IntStream.range(0, items.size())
                .mapToObj(index -> new WishlistResponse(items.get(index).getId(), summaries.get(index), items.get(index).getCreatedAt()))
                .toList();
    }
    @Transactional
    public WishlistResponse add(UUID userId, UUID productId) {
        WishlistEntity existing = repository.findByUser_IdAndProduct_Id(userId, productId).orElse(null);
        if (existing != null) return response(existing);
        ProductEntity product = productRepository.findDetailedById(productId)
                .filter(value -> value.getStatus() == ProductStatus.ACTIVE)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "PRODUCT_NOT_FOUND", "Không tìm thấy sản phẩm"));
        UserEntity user = userRepository.findById(userId).orElseThrow(() -> new ApiException(
                HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "Không tìm thấy người dùng"));
        return response(repository.save(WishlistEntity.create(user, product, Instant.now(clock))));
    }
    @Transactional
    public void remove(UUID userId, UUID productId) { repository.deleteByUser_IdAndProduct_Id(userId, productId); }
    private WishlistResponse response(WishlistEntity item) {
        return new WishlistResponse(item.getId(), assembler.summaries(List.of(item.getProduct())).getFirst(), item.getCreatedAt());
    }
}
