package com.shoppew.product.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;

@Entity
@Table(name = "product_attribute_values")
public class ProductAttributeValueEntity {

    @EmbeddedId
    private ProductAttributeValueId id;

    @MapsId("productId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private ProductEntity product;

    @MapsId("attributeId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "attribute_id", nullable = false)
    private ProductAttributeEntity attribute;

    @Column(name = "value_text", nullable = false, length = 1000)
    private String valueText;

    protected ProductAttributeValueEntity() {}

    public static ProductAttributeValueEntity create(
            ProductEntity product,
            ProductAttributeEntity attribute,
            String valueText) {
        ProductAttributeValueEntity value = new ProductAttributeValueEntity();
        value.id = new ProductAttributeValueId(product.getId(), attribute.getId());
        value.product = product;
        value.attribute = attribute;
        value.valueText = valueText;
        return value;
    }

    public ProductAttributeEntity getAttribute() { return attribute; }
    public String getValueText() { return valueText; }

    public void update(String valueText) {
        this.valueText = valueText;
    }
}
