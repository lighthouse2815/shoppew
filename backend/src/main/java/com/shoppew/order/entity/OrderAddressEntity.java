package com.shoppew.order.entity;

import com.shoppew.address.entity.UserAddressEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.Table;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.springframework.data.domain.Persistable;

@Entity
@Table(name = "order_addresses")
public class OrderAddressEntity implements Persistable<UUID> {

    @Id
    @Column(name = "order_id")
    private UUID orderId;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id")
    private OrderEntity order;

    @Column(name = "recipient_name", nullable = false, length = 120)
    private String recipientName;
    @Column(nullable = false, length = 32)
    private String phone;
    @Column(name = "country_code", nullable = false, columnDefinition = "char(2)")
    @JdbcTypeCode(SqlTypes.CHAR)
    private String countryCode;
    @Column(nullable = false, length = 120)
    private String province;
    @Column(nullable = false, length = 120)
    private String district;
    @Column(length = 120)
    private String ward;
    @Column(name = "address_line", nullable = false, length = 255)
    private String addressLine;
    @Column(name = "postal_code", length = 24)
    private String postalCode;

    @jakarta.persistence.Transient
    private boolean newEntity = true;

    protected OrderAddressEntity() {}

    public static OrderAddressEntity snapshot(OrderEntity order, UserAddressEntity source) {
        OrderAddressEntity address = new OrderAddressEntity();
        address.order = order;
        address.orderId = order.getId();
        address.recipientName = source.getRecipientName();
        address.phone = source.getPhone();
        address.countryCode = source.getCountryCode();
        address.province = source.getProvince();
        address.district = source.getDistrict();
        address.ward = source.getWard();
        address.addressLine = source.getAddressLine();
        address.postalCode = source.getPostalCode();
        return address;
    }

    public UUID getOrderId() { return orderId; }
    @Override public UUID getId() { return orderId; }
    @Override public boolean isNew() { return newEntity; }
    public String getRecipientName() { return recipientName; }
    public String getPhone() { return phone; }
    public String getCountryCode() { return countryCode; }
    public String getProvince() { return province; }
    public String getDistrict() { return district; }
    public String getWard() { return ward; }
    public String getAddressLine() { return addressLine; }
    public String getPostalCode() { return postalCode; }

    @PostPersist
    @PostLoad
    private void markNotNew() { newEntity = false; }
}
