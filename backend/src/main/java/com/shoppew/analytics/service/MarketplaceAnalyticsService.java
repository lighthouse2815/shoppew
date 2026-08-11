package com.shoppew.analytics.service;

import com.shoppew.analytics.dto.AdminAnalyticsResponse;
import com.shoppew.analytics.dto.SellerAnalyticsResponse;
import com.shoppew.common.exception.ApiException;
import com.shoppew.shop.service.ShopAccessService;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MarketplaceAnalyticsService {
    private final JdbcTemplate jdbc; private final ShopAccessService access; private final Clock clock;
    public MarketplaceAnalyticsService(JdbcTemplate jdbc, ShopAccessService access, Clock clock) { this.jdbc = jdbc; this.access = access; this.clock = clock; }

    @Transactional(readOnly = true)
    public SellerAnalyticsResponse seller(UUID userId, UUID shopId, Instant from, Instant to) {
        access.requireActiveMember(userId, shopId); Range range = range(from, to);
        var money = jdbc.queryForMap("""
                select coalesce(sum(case when st.transaction_type = 'SALE_AVAILABLE' then st.amount
                                         when st.transaction_type = 'REFUND' then st.amount else 0 end), 0) revenue
                from seller_transactions st where st.shop_id = ? and st.created_at >= ? and st.created_at < ?
                """, shopId, Timestamp.from(range.from), Timestamp.from(range.to));
        var orderStats = jdbc.queryForMap("""
                select count(*) order_count, coalesce(avg(grand_total), 0) average_order
                from orders where shop_id = ? and status in ('COMPLETED','PARTIALLY_REFUNDED','REFUNDED')
                  and completed_at >= ? and completed_at < ?
                """, shopId, Timestamp.from(range.from), Timestamp.from(range.to));
        List<SellerAnalyticsResponse.TopProduct> top = jdbc.query("""
                select oi.product_id, oi.product_name, sum(oi.quantity) quantity, sum(oi.subtotal) revenue
                from order_items oi join orders o on o.id = oi.order_id
                where o.shop_id = ? and o.status in ('COMPLETED','PARTIALLY_REFUNDED','REFUNDED')
                  and o.completed_at >= ? and o.completed_at < ? and oi.product_id is not null
                group by oi.product_id, oi.product_name order by quantity desc, revenue desc limit 10
                """, (rs, row) -> new SellerAnalyticsResponse.TopProduct(rs.getObject(1, UUID.class), rs.getString(2),
                rs.getLong(3), rs.getBigDecimal(4)), shopId, Timestamp.from(range.from), Timestamp.from(range.to));
        List<SellerAnalyticsResponse.LowStock> low = jdbc.query("""
                select p.id, pv.id, p.name, pv.name, pv.sku, i.available_quantity, i.low_stock_threshold
                from inventories i join product_variants pv on pv.id = i.variant_id join products p on p.id = pv.product_id
                where p.shop_id = ? and p.status = 'ACTIVE' and pv.status = 'ACTIVE'
                  and i.available_quantity <= i.low_stock_threshold
                order by i.available_quantity, p.name, pv.name limit 50
                """, (rs, row) -> new SellerAnalyticsResponse.LowStock(rs.getObject(1, UUID.class), rs.getObject(2, UUID.class),
                rs.getString(3), rs.getString(4), rs.getString(5), rs.getLong(6), rs.getLong(7)), shopId);
        return new SellerAnalyticsResponse(shopId, range.from, range.to, decimal(money.get("revenue")),
                ((Number) orderStats.get("order_count")).longValue(), decimal(orderStats.get("average_order")), top, low);
    }

    @Transactional(readOnly = true)
    public AdminAnalyticsResponse admin(Instant from, Instant to) {
        Range range = range(from, to); Timestamp start = Timestamp.from(range.from); Timestamp end = Timestamp.from(range.to);
        BigDecimal gmv = jdbc.queryForObject("""
                select coalesce(sum(o.grand_total - coalesce(r.refunded, 0)), 0)
                from orders o left join (
                    select rr.order_id, sum(f.amount) refunded from refunds f
                    join refund_requests rr on rr.id = f.refund_request_id where f.status = 'SUCCEEDED' group by rr.order_id
                ) r on r.order_id = o.id
                where o.status in ('COMPLETED','PARTIALLY_REFUNDED','REFUNDED') and o.completed_at >= ? and o.completed_at < ?
                """, BigDecimal.class, start, end);
        Long completed = jdbc.queryForObject("select count(*) from orders where status in ('COMPLETED','PARTIALLY_REFUNDED','REFUNDED') and completed_at >= ? and completed_at < ?", Long.class, start, end);
        Long newUsers = jdbc.queryForObject("select count(*) from app_users where created_at >= ? and created_at < ?", Long.class, start, end);
        Long activeShops = jdbc.queryForObject("select count(*) from shops where status = 'ACTIVE'", Long.class);
        Long pending = jdbc.queryForObject("select count(*) from products where status = 'PENDING_REVIEW'", Long.class);
        BigDecimal refundVolume = jdbc.queryForObject("select coalesce(sum(amount), 0) from refunds where status = 'SUCCEEDED' and completed_at >= ? and completed_at < ?", BigDecimal.class, start, end);
        return new AdminAnalyticsResponse(range.from, range.to, gmv, completed, newUsers, activeShops, pending, refundVolume);
    }

    private Range range(Instant from, Instant to) {
        Instant resolvedTo = to == null ? Instant.now(clock) : to;
        Instant resolvedFrom = from == null ? resolvedTo.minus(Duration.ofDays(30)) : from;
        if (!resolvedFrom.isBefore(resolvedTo) || Duration.between(resolvedFrom, resolvedTo).compareTo(Duration.ofDays(366)) > 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "ANALYTICS_RANGE_INVALID", "Analytics range must be positive and at most 366 days");
        }
        return new Range(resolvedFrom, resolvedTo);
    }
    private BigDecimal decimal(Object value) { return value instanceof BigDecimal decimal ? decimal : new BigDecimal(value.toString()); }
    private record Range(Instant from, Instant to) {}
}
