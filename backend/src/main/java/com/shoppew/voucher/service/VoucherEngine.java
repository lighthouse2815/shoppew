package com.shoppew.voucher.service;

import com.shoppew.checkout.entity.CheckoutGroupEntity;
import com.shoppew.common.exception.ApiException;
import com.shoppew.order.entity.OrderEntity;
import com.shoppew.payment.entity.PaymentProviderType;
import com.shoppew.user.entity.UserEntity;
import com.shoppew.voucher.dto.VoucherApplicationResponse;
import com.shoppew.voucher.entity.DiscountType;
import com.shoppew.voucher.entity.VoucherEntity;
import com.shoppew.voucher.entity.VoucherOwnerType;
import com.shoppew.voucher.entity.VoucherType;
import com.shoppew.voucher.entity.VoucherUsageEntity;
import com.shoppew.voucher.entity.VoucherUsageStatus;
import com.shoppew.voucher.repository.VoucherRepository;
import com.shoppew.voucher.repository.VoucherUsageRepository;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class VoucherEngine {
    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);
    private final VoucherRepository voucherRepository;
    private final VoucherUsageRepository usageRepository;
    private final EntityManager entityManager;
    private final Clock clock;

    public VoucherEngine(VoucherRepository voucherRepository, VoucherUsageRepository usageRepository,
            EntityManager entityManager, Clock clock) {
        this.voucherRepository = voucherRepository; this.usageRepository = usageRepository;
        this.entityManager = entityManager; this.clock = clock;
    }

    @Transactional(readOnly = true)
    public VoucherQuote quote(UUID userId, Set<String> requestedCodes, PaymentProviderType payment,
            String currency, List<ShopInput> shops) {
        Set<String> codes = normalizeCodes(requestedCodes);
        if (codes.isEmpty()) return VoucherQuote.empty();
        List<VoucherEntity> vouchers = voucherRepository.findDetailedByCodes(codes);
        if (vouchers.size() != codes.size()) throw invalid("VOUCHER_NOT_FOUND", "Không tìm thấy một hoặc nhiều voucher");
        vouchers.sort(Comparator.comparing(VoucherEntity::getCode));
        Instant now = Instant.now(clock);
        Map<UUID, BigDecimal> alreadyApplied = new HashMap<>();
        List<Application> applications = new ArrayList<>();
        for (VoucherEntity voucher : vouchers) {
            validateAvailable(voucher, userId, payment, currency, shops, now);
            Map<UUID, BigDecimal> eligible = eligibleByShop(voucher, shops);
            BigDecimal spend = spendForMinimum(voucher, shops);
            if (spend.compareTo(voucher.getMinimumSpend()) < 0) {
                throw invalid("VOUCHER_MINIMUM_SPEND", "Đơn hàng chưa đạt giá trị tối thiểu cho voucher " + voucher.getCode());
            }
            BigDecimal eligibleTotal = sum(eligible.values());
            if (eligibleTotal.signum() <= 0) throw invalid("VOUCHER_NOT_APPLICABLE", "Voucher không áp dụng cho sản phẩm đã chọn");
            BigDecimal discount = voucher.getDiscountType() == DiscountType.FIXED
                    ? voucher.getDiscountValue() : eligibleTotal.multiply(voucher.getDiscountValue())
                            .divide(HUNDRED, 2, RoundingMode.HALF_UP);
            if (voucher.getMaxDiscount() != null) discount = discount.min(voucher.getMaxDiscount());
            discount = money(discount.min(eligibleTotal));
            Map<UUID, BigDecimal> allocation = allocate(discount, eligible);
            Map<UUID, BigDecimal> capped = new LinkedHashMap<>();
            for (ShopInput shop : shops) {
                BigDecimal proposed = allocation.getOrDefault(shop.shopId(), zero());
                BigDecimal remaining = shop.itemsSubtotal().add(shop.shippingFee())
                        .subtract(alreadyApplied.getOrDefault(shop.shopId(), zero())).max(BigDecimal.ZERO);
                BigDecimal applied = money(proposed.min(remaining));
                if (applied.signum() > 0) {
                    capped.put(shop.shopId(), applied);
                    alreadyApplied.merge(shop.shopId(), applied, BigDecimal::add);
                }
            }
            BigDecimal appliedTotal = sum(capped.values());
            if (appliedTotal.signum() <= 0) throw invalid("VOUCHER_NOT_APPLICABLE", "Voucher không còn giá trị giảm trên checkout này");
            applications.add(new Application(voucher, voucher.getUpdatedAt(), Map.copyOf(capped), appliedTotal));
        }
        return VoucherQuote.of(applications);
    }

    @Transactional
    public void reserve(VoucherQuote quote, UserEntity user, CheckoutGroupEntity checkout,
            Map<UUID, OrderEntity> ordersByShop, String currency) {
        Instant now = Instant.now(clock);
        List<Application> ordered = quote.applications().stream()
                .sorted(Comparator.comparing(application -> application.voucher().getId())).toList();
        for (Application application : ordered) {
            if (entityManager.contains(application.voucher())) entityManager.detach(application.voucher());
            VoucherEntity voucher = voucherRepository.findLocked(application.voucher().getId())
                    .orElseThrow(() -> invalid("VOUCHER_NOT_FOUND", "Voucher không còn tồn tại"));
            if (!voucher.getUpdatedAt().equals(application.observedUpdatedAt())) {
                throw new ApiException(HttpStatus.CONFLICT, "VOUCHER_CHANGED_RETRY", "Voucher vừa thay đổi, vui lòng xem lại checkout");
            }
            if (!voucher.availableAt(now)) throw invalid("VOUCHER_UNAVAILABLE", "Voucher đã hết hạn hoặc hết lượt");
            if (usageRepository.countActiveApplications(voucher.getId(), user.getId()) >= voucher.getPerUserLimit()) {
                throw invalid("VOUCHER_USER_LIMIT", "Bạn đã dùng hết lượt của voucher " + voucher.getCode());
            }
            voucher.reserveOne(now);
            application.discountByShop().forEach((shopId, amount) -> {
                OrderEntity order = ordersByShop.get(shopId);
                if (order == null) throw new IllegalStateException("Voucher allocation references a missing order");
                usageRepository.save(VoucherUsageEntity.reserve(voucher, user, checkout, order, amount, currency, now));
            });
        }
    }

    @Transactional
    public void consumeCheckout(UUID checkoutId) {
        Instant now = Instant.now(clock);
        usageRepository.findAllByCheckout(checkoutId).forEach(usage -> usage.consume(now));
    }

    @Transactional
    public void releaseCheckout(UUID checkoutId) { release(usageRepository.findAllByCheckout(checkoutId)); }

    @Transactional
    public void releaseOrder(UUID orderId) { release(usageRepository.findAllByOrder(orderId)); }

    @Transactional(readOnly = true)
    public List<VoucherApplicationResponse> responses(UUID checkoutId) {
        Map<UUID, List<VoucherUsageEntity>> grouped = usageRepository.findAllByCheckout(checkoutId).stream()
                .filter(usage -> usage.getStatus() != VoucherUsageStatus.RELEASED)
                .collect(Collectors.groupingBy(VoucherUsageEntity::getVoucherId));
        return grouped.values().stream().map(usages -> {
            VoucherEntity voucher = usages.getFirst().getVoucher();
            return new VoucherApplicationResponse(voucher.getId(), voucher.getCode(), voucher.getName(),
                    voucher.getVoucherType(), sum(usages.stream().map(VoucherUsageEntity::getDiscountAmount).toList()),
                    voucher.getCurrency());
        }).sorted(Comparator.comparing(VoucherApplicationResponse::code)).toList();
    }

    private void release(List<VoucherUsageEntity> usages) {
        Instant now = Instant.now(clock);
        Map<UUID, List<VoucherUsageEntity>> byVoucher = usages.stream()
                .filter(usage -> usage.getStatus() != VoucherUsageStatus.RELEASED)
                .collect(Collectors.groupingBy(VoucherUsageEntity::getVoucherId));
        for (List<VoucherUsageEntity> group : byVoucher.values()) {
            UUID voucherId = group.getFirst().getVoucherId();
            group.forEach(usage -> usage.release(now));
            entityManager.flush();
            UUID checkoutId = group.getFirst().getCheckoutGroupId();
            boolean remaining = usageRepository.existsByVoucher_IdAndCheckoutGroup_IdAndStatusIn(
                    voucherId, checkoutId, List.of(VoucherUsageStatus.RESERVED, VoucherUsageStatus.CONSUMED));
            if (!remaining) {
                VoucherEntity referenced = group.getFirst().getVoucher();
                if (entityManager.contains(referenced)) entityManager.detach(referenced);
                VoucherEntity voucher = voucherRepository.findLocked(voucherId)
                        .orElseThrow(() -> new IllegalStateException("Voucher usage references a missing voucher"));
                voucher.releaseOne(now);
            }
        }
    }

    private void validateAvailable(VoucherEntity voucher, UUID userId, PaymentProviderType payment,
            String currency, List<ShopInput> shops, Instant now) {
        if (!voucher.availableAt(now)) throw invalid("VOUCHER_UNAVAILABLE", "Voucher đã hết hạn, chưa bắt đầu hoặc hết lượt");
        if (!voucher.getCurrency().equals(currency)) throw invalid("VOUCHER_CURRENCY_MISMATCH", "Voucher không cùng tiền tệ checkout");
        if (!voucher.getPaymentProviders().isEmpty() && !voucher.getPaymentProviders().contains(payment))
            throw invalid("VOUCHER_PAYMENT_RESTRICTED", "Voucher không áp dụng cho phương thức thanh toán này");
        if (voucher.getShopId() != null && shops.stream().noneMatch(shop -> shop.shopId().equals(voucher.getShopId())))
            throw invalid("VOUCHER_SHOP_RESTRICTED", "Voucher không thuộc shop trong checkout");
        if (usageRepository.countActiveApplications(voucher.getId(), userId) >= voucher.getPerUserLimit())
            throw invalid("VOUCHER_USER_LIMIT", "Bạn đã dùng hết lượt của voucher " + voucher.getCode());
    }

    private Map<UUID, BigDecimal> eligibleByShop(VoucherEntity voucher, List<ShopInput> shops) {
        Set<UUID> products = voucher.getProducts().stream().map(value -> value.getId()).collect(Collectors.toSet());
        Set<UUID> categories = voucher.getCategories().stream().map(value -> value.getId()).collect(Collectors.toSet());
        Map<UUID, BigDecimal> eligible = new LinkedHashMap<>();
        for (ShopInput shop : shops) {
            if (voucher.getShopId() != null && !voucher.getShopId().equals(shop.shopId())) continue;
            BigDecimal base = switch (voucher.getVoucherType()) {
                case SHIPPING -> shop.shippingFee();
                case PRODUCT -> sum(shop.lines().stream().filter(line -> products.contains(line.productId()))
                        .map(LineInput::subtotal).toList());
                case CATEGORY -> sum(shop.lines().stream().filter(line -> categories.contains(line.categoryId()))
                        .map(LineInput::subtotal).toList());
                case PLATFORM, SHOP -> shop.itemsSubtotal();
            };
            if (base.signum() > 0) eligible.put(shop.shopId(), money(base));
        }
        return eligible;
    }

    private BigDecimal spendForMinimum(VoucherEntity voucher, List<ShopInput> shops) {
        return sum(shops.stream().filter(shop -> voucher.getShopId() == null || voucher.getShopId().equals(shop.shopId()))
                .map(ShopInput::itemsSubtotal).toList());
    }

    private Map<UUID, BigDecimal> allocate(BigDecimal total, Map<UUID, BigDecimal> weights) {
        Map<UUID, BigDecimal> result = new LinkedHashMap<>();
        BigDecimal weightTotal = sum(weights.values());
        BigDecimal remaining = total;
        List<Map.Entry<UUID, BigDecimal>> entries = new ArrayList<>(weights.entrySet());
        for (int index = 0; index < entries.size(); index++) {
            var entry = entries.get(index);
            BigDecimal value = index == entries.size() - 1 ? remaining
                    : money(total.multiply(entry.getValue()).divide(weightTotal, 8, RoundingMode.HALF_UP));
            value = value.min(entry.getValue()).min(remaining);
            result.put(entry.getKey(), money(value)); remaining = remaining.subtract(value);
        }
        return result;
    }

    private Set<String> normalizeCodes(Set<String> requested) {
        if (requested == null || requested.isEmpty()) return Set.of();
        if (requested.size() > 5) throw invalid("TOO_MANY_VOUCHERS", "Mỗi checkout hỗ trợ tối đa 5 voucher");
        Set<String> result = requested.stream().map(value -> value.strip().toUpperCase(Locale.ROOT))
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (result.stream().anyMatch(String::isBlank)) throw invalid("INVALID_VOUCHER_CODE", "Mã voucher không hợp lệ");
        return result;
    }
    private BigDecimal sum(Collection<BigDecimal> values) { return values.stream().reduce(zero(), BigDecimal::add).setScale(2, RoundingMode.HALF_UP); }
    private BigDecimal money(BigDecimal value) { return value.setScale(2, RoundingMode.HALF_UP); }
    private BigDecimal zero() { return BigDecimal.ZERO.setScale(2); }
    private ApiException invalid(String code, String message) { return new ApiException(HttpStatus.CONFLICT, code, message); }

    public record LineInput(UUID productId, UUID categoryId, BigDecimal subtotal) {}
    public record ShopInput(UUID shopId, List<LineInput> lines, BigDecimal itemsSubtotal, BigDecimal shippingFee) {}
    public record Application(VoucherEntity voucher, Instant observedUpdatedAt,
            Map<UUID, BigDecimal> discountByShop, BigDecimal totalDiscount) {}
    public record VoucherQuote(List<Application> applications, BigDecimal totalDiscount,
            Map<UUID, BigDecimal> shopOwnerDiscounts, Map<UUID, BigDecimal> platformDiscounts,
            List<VoucherApplicationResponse> responses) {
        static VoucherQuote empty() { return new VoucherQuote(List.of(), BigDecimal.ZERO.setScale(2), Map.of(), Map.of(), List.of()); }
        static VoucherQuote of(List<Application> applications) {
            Map<UUID, BigDecimal> shop = new HashMap<>(); Map<UUID, BigDecimal> platform = new HashMap<>();
            List<VoucherApplicationResponse> responses = new ArrayList<>();
            BigDecimal total = BigDecimal.ZERO.setScale(2);
            for (Application application : applications) {
                Map<UUID, BigDecimal> target = application.voucher().getOwnerType() == VoucherOwnerType.SHOP ? shop : platform;
                application.discountByShop().forEach((id, amount) -> target.merge(id, amount, BigDecimal::add));
                total = total.add(application.totalDiscount());
                responses.add(new VoucherApplicationResponse(application.voucher().getId(), application.voucher().getCode(),
                        application.voucher().getName(), application.voucher().getVoucherType(),
                        application.totalDiscount(), application.voucher().getCurrency()));
            }
            responses.sort(Comparator.comparing(VoucherApplicationResponse::code));
            return new VoucherQuote(List.copyOf(applications), total.setScale(2), Map.copyOf(shop),
                    Map.copyOf(platform), List.copyOf(responses));
        }
    }
}
