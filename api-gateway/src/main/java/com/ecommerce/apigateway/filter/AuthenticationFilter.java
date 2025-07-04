package com.ecommerce.apigateway.filter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.SignatureException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.function.Predicate;

/**
 * Bu global filtre, gelen tüm istekleri yakalar ve JWT tabanlı kimlik doğrulaması yapar.
 * Bu GÜNCELLENMİŞ versiyon, performansı ve dayanıklılığı artırmak için JWT'yi YEREL olarak doğrular.
 */
@Component
public class AuthenticationFilter implements GlobalFilter, Ordered {

    private static final Logger logger = LoggerFactory.getLogger(AuthenticationFilter.class);

    // application.yml dosyasındaki jwt.secret değerini bu değişkene enjekte ediyoruz.
    @Value("${jwt.secret}")
    private String jwtSecret;

    // Kimlik doğrulaması gerektirmeyen, halka açık yolların listesi.
    public static final List<String> openApiEndpoints = List.of(
            "/api/auth/login",
            "/api/auth/register"
            // Not: Ürün listeleme gibi bazı GET isteklerini de buraya ekleyebilirsiniz.
    );

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();

        // İsteğin yolunun halka açık olup olmadığını kontrol ediyoruz.
        Predicate<ServerHttpRequest> isApiSecured = r -> openApiEndpoints.stream()
                .noneMatch(uri -> r.getURI().getPath().startsWith(uri));

        if (isApiSecured.test(request)) {
            // Güvenli bir endpoint ise token kontrolü yapılır.
            logger.info("Güvenli endpoint'e istek geldi, token kontrol ediliyor: {}", request.getURI().getPath());

            if (!request.getHeaders().containsKey(HttpHeaders.AUTHORIZATION)) {
                return this.onError(exchange, "Authorization başlığı eksik.", HttpStatus.UNAUTHORIZED);
            }

            final String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return this.onError(exchange, "Geçersiz Authorization başlığı formatı.", HttpStatus.UNAUTHORIZED);
            }

            // "Bearer " önekini (7 karakter) kaldırarak token'ı alıyoruz.
            final String token = authHeader.substring(7);

            try {
                // Token'ı yerel olarak, paylaşılan gizli anahtarla doğruluyoruz.
                Claims claims = Jwts.parserBuilder()
                        .setSigningKey(jwtSecret.getBytes())
                        .build()
                        .parseClaimsJws(token)
                        .getBody();

                // Token'dan kullanıcı ID'sini alıp isteğin başlığına ekliyoruz.
                String userId = claims.getSubject();
                if (userId == null) {
                    throw new RuntimeException("Token içinde kullanıcı ID (subject) bulunamadı.");
                }

                ServerHttpRequest modifiedRequest = exchange.getRequest().mutate()
                        .header("X-User-Id", userId)
                        .build();

                logger.info("Token yerel olarak doğrulandı. Kullanıcı ID: {}. İstek devam ediyor.", userId);

                // İsteğin, modifiye edilmiş haliyle devam etmesine izin veriyoruz.
                return chain.filter(exchange.mutate().request(modifiedRequest).build());

            } catch (ExpiredJwtException e) {
                logger.warn("Token'ın süresi dolmuş: {}", e.getMessage());
                return this.onError(exchange, "Token süresi dolmuş.", HttpStatus.UNAUTHORIZED);
            } catch (SignatureException e) {
                logger.warn("Token imzası geçersiz: {}", e.getMessage());
                return this.onError(exchange, "Geçersiz imza.", HttpStatus.UNAUTHORIZED);
            } catch (Exception e) {
                logger.error("Bilinmeyen bir token hatası oluştu: {}", e.getMessage());
                return this.onError(exchange, "Geçersiz veya hatalı token.", HttpStatus.UNAUTHORIZED);
            }
        }

        // Halka açık endpoint ise kontrol yapmadan devam ediyoruz.
        logger.info("Herkese açık endpoint'e istek geldi, devam ediliyor: {}", request.getURI().getPath());
        return chain.filter(exchange);
    }

    private Mono<Void> onError(ServerWebExchange exchange, String errMessage, HttpStatus httpStatus) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(httpStatus);
        // Hata mesajını daha anlaşılır JSON formatında döndürelim
        exchange.getResponse().getHeaders().add("Content-Type", "application/json; charset=utf-8");
        String errorJson = "{\"status\":\"error\",\"message\":\"" + errMessage + "\"}";
        logger.warn("İstek reddedildi. Sebep: {}, HTTP Status: {}", errMessage, httpStatus);
        return response.writeWith(Mono.just(response.bufferFactory().wrap(errorJson.getBytes())));
    }

    @Override
    public int getOrder() {
        // Bu filtrenin diğer Gateway filtrelerinden önce çalışması için yüksek öncelik veriyoruz.
        return -1;
    }
}
