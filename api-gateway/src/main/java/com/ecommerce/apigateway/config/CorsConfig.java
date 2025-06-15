package com.ecommerce.apigateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
public class CorsConfig {

    /**
     * Bu Bean, API Gateway için global CORS (Cross-Origin Resource Sharing) ayarlarını yapar.
     * React uygulamamızın (örn: localhost:3778) Gateway'e (localhost:8080) istek atabilmesini sağlar.
     */
    @Bean
    public CorsWebFilter corsWebFilter() {
        CorsConfiguration corsConfig = new CorsConfiguration();

        // React uygulamasının çalıştığı adrese izin veriyoruz.
        // Üretime geçtiğinde buraya gerçek domain adınızı yazmanız gerekir.
        corsConfig.setAllowedOrigins(List.of("http://localhost:3778"));

        // İzin verilen HTTP metodları
        corsConfig.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));

        // İzin verilen HTTP başlıkları
        corsConfig.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-Correlation-ID"));

        // Tarayıcının 'Authorization' gibi başlıkları okumasına izin verir
        corsConfig.setExposedHeaders(List.of("Authorization"));

        // Tarayıcının kimlik bilgileriyle (cookie vb.) istek yapmasına izin verir (gerekliyse)
        corsConfig.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        // Bu konfigürasyonu tüm yollar ('/**') için geçerli kılıyoruz.
        source.registerCorsConfiguration("/**", corsConfig);

        return new CorsWebFilter(source);
    }
}