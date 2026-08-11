package com.shoppew.product.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@Embeddable
public class ProductAttributeValueId implements Serializable {

    @Column(name = "product_id")
    private UUID productId;

    @Column(name = "attribute_id")
    private UUID attributeId;

    protected ProductAttributeValueId() {}

    public ProductAttributeValueId(UUID productId, UUID attributeId) {
        this.productId = productId;
        this.attributeId = attributeId;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (!(object instanceof ProductAttributeValueId that)) return false;
        return Objects.equals(productId, that.productId) && Objects.equals(attributeId, that.attributeId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(productId, attributeId);
    }
}
