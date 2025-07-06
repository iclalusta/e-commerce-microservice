package com.example.productservice.dto;

import java.io.Serializable;
import java.math.BigDecimal;

// Lombok ek açıklamaları (Getter, Setter, vb. için)
// Projenizde Lombok bağımlılığı varsa kullanabilirsiniz.
// Yoksa manuel olarak getter/setter ekleyin.
public class ProductUpdatedEvent implements Serializable {
    private Long productId;
    private String name;
    private BigDecimal price;
    private Integer stock;

    // Default constructor (JSON serileştirme için gerekli)
    public ProductUpdatedEvent() {
    }

    public ProductUpdatedEvent(Long productId, String name, BigDecimal price, Integer stock) {
        this.productId = productId;
        this.name = name;
        this.price = price;
        this.stock = stock;
    }

    // Getter ve Setter metotları...
    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public Integer getStock() { return stock; }
    public void setStock(Integer stock) { this.stock = stock; }
}