package com.shoppew.finance.service;

import com.shoppew.common.api.PageResponse;
import com.shoppew.finance.dto.SellerBalanceResponse;
import com.shoppew.finance.dto.SellerTransactionResponse;
import com.shoppew.finance.entity.BalanceBucket;
import com.shoppew.finance.entity.SellerBalanceEntity;
import com.shoppew.finance.entity.SellerTransactionEntity;
import com.shoppew.finance.entity.SellerTransactionType;
import com.shoppew.finance.repository.SellerBalanceRepository;
import com.shoppew.finance.repository.SellerTransactionRepository;
import com.shoppew.order.entity.OrderEntity;
import com.shoppew.order.entity.OrderStatus;
import com.shoppew.order.event.OrderStatusChangedEvent;
import com.shoppew.order.repository.OrderRepository;
import com.shoppew.refund.entity.RefundEntity;
import com.shoppew.shop.repository.ShopRepository;
import com.shoppew.shop.service.ShopAccessService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SellerFinanceService {
    private final SellerBalanceRepository balances;
    private final SellerTransactionRepository transactions;
    private final OrderRepository orders;
    private final ShopRepository shops;
    private final ShopAccessService access;
    private final Clock clock;
    private final BigDecimal platformFeeRate;

    public SellerFinanceService(SellerBalanceRepository balances, SellerTransactionRepository transactions,
            OrderRepository orders, ShopRepository shops, ShopAccessService access, Clock clock,
            @Value("${app.finance.platform-fee-rate:0.05}") BigDecimal platformFeeRate) {
        this.balances = balances; this.transactions = transactions; this.orders = orders; this.shops = shops;
        this.access = access; this.clock = clock; this.platformFeeRate = platformFeeRate;
        if (platformFeeRate.signum() < 0 || platformFeeRate.compareTo(BigDecimal.ONE) >= 0) {
            throw new IllegalArgumentException("app.finance.platform-fee-rate must be in [0,1)");
        }
    }

    @EventListener
    @Transactional
    public void onOrderStatusChanged(OrderStatusChangedEvent event) {
        if (event.to() != OrderStatus.DELIVERED && event.to() != OrderStatus.COMPLETED) return;
        OrderEntity order = orders.findById(event.orderId()).orElseThrow();
        ensurePending(order);
        if (event.to() == OrderStatus.COMPLETED) makeAvailable(order);
    }

    @Transactional
    public void recordRefund(OrderEntity order, RefundEntity refund) {
        String reference = "refund:" + refund.getId();
        if (transactions.existsByShop_IdAndTransactionTypeAndReferenceKey(
                order.getShopId(), SellerTransactionType.REFUND, reference)) return;
        Instant now = Instant.now(clock);
        SellerBalanceEntity balance = lockedBalance(order, now);
        balance.deductRefund(refund.getSellerChargeAmount(), now);
        transactions.save(SellerTransactionEntity.create(order.getShop(), SellerTransactionType.REFUND,
                refund.getSellerChargeAmount().negate(), order.getCurrency(), BalanceBucket.AVAILABLE,
                order, refund, reference, "Seller charge for refund " + refund.getId(), now));
    }

    @Transactional
    public SellerBalanceResponse balance(UUID userId, UUID shopId) {
        access.requireActiveMember(userId, shopId);
        SellerBalanceEntity balance = balances.findById(shopId).orElseGet(() -> balances.saveAndFlush(
                SellerBalanceEntity.create(shops.findById(shopId).orElseThrow(), "VND", Instant.now(clock))));
        return response(balance);
    }

    @Transactional(readOnly = true)
    public PageResponse<SellerTransactionResponse> transactions(UUID userId, UUID shopId, int page, int size) {
        access.requireActiveMember(userId, shopId);
        return PageResponse.from(transactions.findAllByShop_Id(shopId,
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))), this::response);
    }

    public BigDecimal feeFor(OrderEntity order) {
        return order.getItemsSubtotal().subtract(order.getShopDiscountTotal())
                .multiply(platformFeeRate).setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal sellerNet(OrderEntity order) {
        return order.getItemsSubtotal().subtract(order.getShopDiscountTotal()).subtract(feeFor(order));
    }

    private void ensurePending(OrderEntity order) {
        String grossReference = "order:" + order.getId() + ":gross";
        if (transactions.existsByShop_IdAndTransactionTypeAndReferenceKey(
                order.getShopId(), SellerTransactionType.SALE_PENDING, grossReference)) return;
        Instant now = Instant.now(clock);
        SellerBalanceEntity balance = lockedBalance(order, now);
        BigDecimal fee = feeFor(order);
        BigDecimal net = sellerNet(order);
        balance.addPending(net, now);
        transactions.save(SellerTransactionEntity.create(order.getShop(), SellerTransactionType.SALE_PENDING,
                order.getItemsSubtotal(), order.getCurrency(), BalanceBucket.PENDING, order, null,
                grossReference, "Gross merchandise sale snapshot", now));
        if (order.getShopDiscountTotal().signum() > 0) {
            transactions.save(SellerTransactionEntity.create(order.getShop(), SellerTransactionType.DISCOUNT_ALLOCATION,
                    order.getShopDiscountTotal().negate(), order.getCurrency(), BalanceBucket.PENDING, order, null,
                    "order:" + order.getId() + ":shop-discount", "Seller-funded discount snapshot", now));
        }
        if (fee.signum() > 0) {
            transactions.save(SellerTransactionEntity.create(order.getShop(), SellerTransactionType.PLATFORM_FEE,
                    fee.negate(), order.getCurrency(), BalanceBucket.PENDING, order, null,
                    "order:" + order.getId() + ":platform-fee", "Platform fee rate snapshot " + platformFeeRate, now));
        }
    }

    private void makeAvailable(OrderEntity order) {
        String reference = "order:" + order.getId() + ":available";
        if (transactions.existsByShop_IdAndTransactionTypeAndReferenceKey(
                order.getShopId(), SellerTransactionType.SALE_AVAILABLE, reference)) return;
        Instant now = Instant.now(clock);
        BigDecimal net = sellerNet(order);
        SellerBalanceEntity balance = lockedBalance(order, now);
        balance.makeAvailable(net, now);
        transactions.save(SellerTransactionEntity.create(order.getShop(), SellerTransactionType.SALE_AVAILABLE,
                net, order.getCurrency(), BalanceBucket.AVAILABLE, order, null, reference,
                "Completed order seller net released", now));
    }

    private SellerBalanceEntity lockedBalance(OrderEntity order, Instant now) {
        return balances.findLocked(order.getShopId()).orElseGet(() -> {
            balances.saveAndFlush(SellerBalanceEntity.create(order.getShop(), order.getCurrency(), now));
            return balances.findLocked(order.getShopId()).orElseThrow();
        });
    }

    private SellerBalanceResponse response(SellerBalanceEntity entity) {
        return new SellerBalanceResponse(entity.getShopId(), entity.getCurrency(), entity.getPendingAmount(),
                entity.getAvailableAmount(), entity.getHeldAmount(), entity.getPaidOutAmount(), entity.getUpdatedAt());
    }
    private SellerTransactionResponse response(SellerTransactionEntity entity) {
        return new SellerTransactionResponse(entity.getId(), entity.getTransactionType(), entity.getAmount(),
                entity.getCurrency(), entity.getBalanceBucket(), entity.getOrderId(), entity.getRefundId(),
                entity.getReferenceKey(), entity.getDescription(), entity.getCreatedAt());
    }
}
