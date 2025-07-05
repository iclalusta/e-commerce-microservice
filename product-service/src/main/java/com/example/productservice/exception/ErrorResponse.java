package com.example.productservice.exception;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor // Gerekli olan tüm alanları içeren constructor'ı oluşturur
@NoArgsConstructor  // Boş constructor'ı oluşturur
public class ErrorResponse {
    private LocalDateTime timestamp;
    private int status;
    private String error;
    private String message;
    private String path;
}