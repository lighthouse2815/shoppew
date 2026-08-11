package com.shoppew.product.entity;

import com.shoppew.catalog.entity.CategoryEntity;
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
import java.util.UUID;

@Entity
@Table(name = "product_attributes")
public class ProductAttributeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private CategoryEntity category;

    @Column(nullable = false, length = 120)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "value_type", nullable = false, length = 24)
    private AttributeValueType valueType;

    @Column(name = "is_required", nullable = false)
    private boolean required;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    protected ProductAttributeEntity() {}

    public static ProductAttributeEntity create(
            CategoryEntity category,
            String name,
            AttributeValueType valueType,
            boolean required,
            int sortOrder) {
        ProductAttributeEntity attribute = new ProductAttributeEntity();
        attribute.category = category;
        attribute.name = name;
        attribute.valueType = valueType;
        attribute.required = required;
        attribute.sortOrder = sortOrder;
        return attribute;
    }

    public UUID getId() { return id; }
    public UUID getCategoryId() { return category == null ? null : category.getId(); }
    public String getName() { return name; }
    public AttributeValueType getValueType() { return valueType; }
    public boolean isRequired() { return required; }
    public int getSortOrder() { return sortOrder; }

    public void update(String name, AttributeValueType valueType, boolean required, int sortOrder) {
        this.name = name;
        this.valueType = valueType;
        this.required = required;
        this.sortOrder = sortOrder;
    }
}
