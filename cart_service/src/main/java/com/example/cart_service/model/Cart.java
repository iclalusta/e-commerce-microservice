package com.example.cart_service.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Document(collection = "carts")
public class Cart {
    @Id
    private String id;
    private String cartIdentifier;
    @Builder.Default
    private List<CartItem> items = new ArrayList<>();
    private LocalDateTime lastModifiedDate;
}
