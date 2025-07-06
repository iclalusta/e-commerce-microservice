package com.example.cart_service.event;

import java.math.BigDecimal;
import java.io.Serializable;

// Not: Bu sınıfın içeriği product-service'teki DTO ile birebir aynı olmalıdır.
public class ProductUpdatedEvent implements Serializable {

    // ALAN ADLARI DEĞİŞTİRİLDİ
    private Long productId;
    private String name;
    private BigDecimal price;
    private Integer stock;

    // JSON dönüştürücü için boş constructor
    public ProductUpdatedEvent() {
    }

    // Getter ve Setter metotları
    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public Integer getStock() {
        return stock;
    }

    public void setStock(Integer stock) {
        this.stock = stock;
    }
}