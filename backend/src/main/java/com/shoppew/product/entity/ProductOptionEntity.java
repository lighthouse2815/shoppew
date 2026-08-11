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
@Table(name = "product_options")
public class ProductOptionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private ProductEntity product;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    protected ProductOptionEntity() {}

    public static ProductOptionEntity create(ProductEntity product, String name, int sortOrder) {
        ProductOptionEntity option = new ProductOptionEntity();
        option.product = product;
        option.name = name;
        option.sortOrder = sortOrder;
        return option;
    }

    public UUID getId() { return id; }
    public UUID getProductId() { return product.getId(); }
    public String getName() { return name; }
    public int getSortOrder() { return sortOrder; }

    public void update(String name, int sortOrder) {
        this.name = name;
        this.sortOrder = sortOrder;
    }
}
