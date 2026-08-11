package com.shoppew.shop.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "shop_addresses")
public class ShopAddressEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "shop_id", nullable = false)
    private ShopEntity shop;

    @Enumerated(EnumType.STRING)
    @Column(name = "address_type", nullable = false, length = 24)
    private ShopAddressType addressType;

    @Column(name = "contact_name", nullable = false, length = 120)
    private String contactName;

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

    @Column(name = "is_default", nullable = false)
    private boolean defaultAddress;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected ShopAddressEntity() {}

    public static ShopAddressEntity create(
            ShopEntity shop,
            ShopAddressType type,
            AddressValues values,
            boolean defaultAddress,
            Instant now) {
        ShopAddressEntity address = new ShopAddressEntity();
        address.shop = shop;
        address.addressType = type;
        address.apply(values);
        address.defaultAddress = defaultAddress;
        address.createdAt = now;
        address.updatedAt = now;
        return address;
    }

    public UUID getId() {
        return id;
    }

    public ShopAddressType getAddressType() {
        return addressType;
    }

    public String getContactName() {
        return contactName;
    }

    public String getPhone() {
        return phone;
    }

    public String getCountryCode() {
        return countryCode;
    }

    public String getProvince() {
        return province;
    }

    public String getDistrict() {
        return district;
    }

    public String getWard() {
        return ward;
    }

    public String getAddressLine() {
        return addressLine;
    }

    public String getPostalCode() {
        return postalCode;
    }

    public boolean isDefaultAddress() {
        return defaultAddress;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void update(ShopAddressType type, AddressValues values, Instant now) {
        addressType = type;
        apply(values);
        updatedAt = now;
    }

    public void makeDefault(Instant now) {
        defaultAddress = true;
        updatedAt = now;
    }

    public void clearDefault(Instant now) {
        defaultAddress = false;
        updatedAt = now;
    }

    private void apply(AddressValues values) {
        contactName = values.contactName();
        phone = values.phone();
        countryCode = values.countryCode();
        province = values.province();
        district = values.district();
        ward = values.ward();
        addressLine = values.addressLine();
        postalCode = values.postalCode();
    }

    public record AddressValues(
            String contactName,
            String phone,
            String countryCode,
            String province,
            String district,
            String ward,
            String addressLine,
            String postalCode) {}
}
