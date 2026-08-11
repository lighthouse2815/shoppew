package com.shoppew.product.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "product_option_values")
public class ProductOptionValueEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "option_id", nullable = false)
    private ProductOptionEntity option;

    @Column(nullable = false, length = 120)
    private String value;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    protected ProductOptionValueEntity() {}

    public static ProductOptionValueEntity create(ProductOptionEntity option, String value, int sortOrder) {
        ProductOptionValueEntity optionValue = new ProductOptionValueEntity();
        optionValue.option = option;
        optionValue.value = value;
        optionValue.sortOrder = sortOrder;
        return optionValue;
    }

    public UUID getId() { return id; }
    public ProductOptionEntity getOption() { return option; }
    public UUID getOptionId() { return option.getId(); }
    public UUID getProductId() { return option.getProductId(); }
    public String getValue() { return value; }
    public int getSortOrder() { return sortOrder; }

    public void update(String value, int sortOrder) {
        this.value = value;
        this.sortOrder = sortOrder;
    }
}
