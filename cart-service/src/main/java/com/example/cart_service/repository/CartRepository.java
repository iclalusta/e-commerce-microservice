package com.example.cart_service.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import com.example.cart_service.model.Cart;

public interface CartRepository extends MongoRepository<Cart, String> {
    Optional<Cart> findByCartIdentifier(String cartIdentifier);
    void deleteByCartIdentifier(String cartIdentifier);
    @Query(value = "{'items.productId': ?0}")
    List<Cart> findByItemsProductId(Long productId);
}
