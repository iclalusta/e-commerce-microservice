package com.example.productservice.service;

import com.example.productservice.dto.ProductUpdatedEvent; // YENİ IMPORT
import com.example.productservice.exception.InsufficientStockException;
import com.example.productservice.exception.ProductNotFoundException;
import com.example.productservice.model.Product;
import com.example.productservice.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.amqp.rabbit.core.RabbitTemplate; // YENİ IMPORT
import org.springframework.beans.factory.annotation.Value; // YENİ IMPORT


import java.util.List;
import java.util.Optional;

@Service
public class ProductService {
    private final ProductRepository productRepository;
    private final RabbitTemplate rabbitTemplate; // YENİ ALAN

    // --- YENİ ALANLAR ---
    @Value("${rabbitmq.exchange.product}")
    private String productExchange;

    @Value("${rabbitmq.routingkey.product.updated}")
    private String productUpdatedRoutingKey;
    // --- YENİ ALANLAR SONU ---



    @Autowired
    public ProductService(ProductRepository productRepository, RabbitTemplate rabbitTemplate) { // CONSTRUCTOR GÜNCELLENDİ
        this.productRepository = productRepository;
        this.rabbitTemplate = rabbitTemplate; // YENİ
    }
    public Product createProduct(Product product) {
        return productRepository.save(product);
    }

    public Optional<Product> getProductById(Long id) {
        return productRepository.findById(id);
    }

    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }

    public void deleteProduct(Long id) {
        productRepository.deleteById(id);
    }

    @Transactional
    public Product decreaseStock(Long id, int quantity) {
        if (quantity <= 0) throw new IllegalArgumentException("Quantity must be positive");
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException("Product not found with id: " + id));
        if (product.getStock() < quantity) {
            throw new InsufficientStockException("Insufficient stock for product id: " + id);
        }
        product.setStock(product.getStock() - quantity);

        Product savedProduct = productRepository.save(product);

        // --- YENİ EKLENEN KISIM: OLAYI YAYINLA ---
        // Stok değişikliği de bir ürün güncellemesidir.
        publishProductUpdatedEvent(savedProduct);
        // --- YENİ EKLENEN KISIM SONU ---

        return savedProduct;
    }

    @Transactional
    public Product increaseStock(Long id, int quantity) {
        if (quantity <= 0) throw new IllegalArgumentException("Quantity must be positive");
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException("Product not found with id: " + id));
        product.setStock(product.getStock() + quantity);

        Product savedProduct = productRepository.save(product);

        // --- YENİ EKLENEN KISIM: OLAYI YAYINLA ---
        publishProductUpdatedEvent(savedProduct);
        // --- YENİ EKLENEN KISIM SONU ---

        return savedProduct;
    }

    public Product updateProduct(Long id, Product updatedProduct) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException("Product not found with id: " + id));
        product.setName(updatedProduct.getName());
        product.setDescription(updatedProduct.getDescription());
        product.setPrice(updatedProduct.getPrice());
        product.setStock(updatedProduct.getStock());
        product.setCategory(updatedProduct.getCategory());
        product.setImageUrl(updatedProduct.getImageUrl());

        Product savedProduct = productRepository.save(product);

        // --- YENİ EKLENEN KISIM: OLAYI YAYINLA ---
        publishProductUpdatedEvent(savedProduct);
        // --- YENİ EKLENEN KISIM SONU ---

        return savedProduct;
    }

    // --- YENİ YARDIMCI METOT ---
    private void publishProductUpdatedEvent(Product product) {
        ProductUpdatedEvent event = new ProductUpdatedEvent(
                product.getId(),
                product.getName(),
                product.getPrice(),
                product.getStock()
        );
        rabbitTemplate.convertAndSend(productExchange, productUpdatedRoutingKey, event);
        System.out.println("ProductUpdatedEvent published for product ID: " + product.getId());
    }
    // --- YENİ YARDIMCI METOT SONU ---
}