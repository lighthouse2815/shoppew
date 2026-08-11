package com.shoppew.shop.service;

import com.shoppew.common.exception.ApiException;
import com.shoppew.shop.entity.ShopMemberEntity;
import com.shoppew.shop.entity.ShopMemberStatus;
import com.shoppew.shop.repository.ShopMemberRepository;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ShopAccessService {

    private final ShopMemberRepository memberRepository;

    public ShopAccessService(ShopMemberRepository memberRepository) {
        this.memberRepository = memberRepository;
    }

    @Transactional(readOnly = true)
    public ShopMemberEntity requireActiveMember(UUID userId, UUID shopId) {
        return memberRepository
                .findByShopIdAndUserIdAndStatus(shopId, userId, ShopMemberStatus.ACTIVE)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.FORBIDDEN,
                        "SHOP_ACCESS_DENIED",
                        "Bạn không có quyền truy cập cửa hàng này"));
    }
}
