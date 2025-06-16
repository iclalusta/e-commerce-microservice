package com.ecommerce.apigateway.filter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
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
 * Bu filtre, gelen tüm istekleri yakalayan global bir filtredir.
 * Görevi, halka açık olmayan endpoint'lere gelen isteklerde JWT kontrolü yapmaktır.
 */
@Component // Bu sınıfın bir Spring bileşeni olarak algılanmasını sağlar
public class AuthenticationFilter implements GlobalFilter, Ordered {

    // Loglama için bir logger nesnesi oluşturuyoruz
    private static final Logger logger = LoggerFactory.getLogger(AuthenticationFilter.class);

    // application.yml dosyasındaki jwt.secret değerini bu değişkene enjekte ediyoruz
    @Value("${jwt.secret}")
    private String jwtSecret;

    // Kimlik doğrulaması gerektirmeyen, herkese açık yolların listesi.
    // Login ve register gibi endpoint'ler token gerektirmez.
    public static final List<String> openApiEndpoints = List.of(
            "/auth/login",
            "/auth/register"
            // Not: Ürün listeleme gibi bazı GET isteklerini de buraya ekleyebilirsiniz.
            // Örn: "/api/products" (sadece GET metodu için kontrol eklemek gerekebilir)
    );

    /**
     * Filtrenin ana mantığının bulunduğu metot.
     * Her istek bu metottan geçer.
     */
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        // Gelen isteği alıyoruz
        ServerHttpRequest request = exchange.getRequest();

        // İsteğin yolunun (path) halka açık endpoint'lerden biri olup olmadığını kontrol ediyoruz
        Predicate<ServerHttpRequest> isApiSecured = r -> openApiEndpoints.stream()
                .noneMatch(uri -> r.getURI().getPath().startsWith(uri));

        // Eğer istek güvenli bir yola yapılıyorsa (yani halka açık değilse)
        if (isApiSecured.test(request)) {
            logger.info("Güvenli endpoint'e istek geldi, token kontrol ediliyor: {}", request.getURI().getPath());

            // 1. Authorization başlığı var mı diye kontrol et
            if (!request.getHeaders().containsKey(HttpHeaders.AUTHORIZATION)) {
                logger.warn("Authorization başlığı eksik.");
                return this.onError(exchange, "Authorization başlığı eksik", HttpStatus.UNAUTHORIZED);
            }

            // 2. Başlıktan "Bearer " ile başlayan token'ı al
            final String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                logger.warn("Authorization başlığı 'Bearer ' ile başlamıyor.");
                return this.onError(exchange, "Geçersiz Authorization başlığı", HttpStatus.UNAUTHORIZED);
            }

            // "Bearer " kısmını (7 karakter) atlayarak sadece token'ı al
            final String token = authHeader; //.substring(7);

            try {
                // 3. Token'ı doğrula ve içindeki bilgileri (claims) al
                Claims claims = Jwts.parserBuilder()
                        .setSigningKey(jwtSecret.getBytes()) // Gizli anahtarla doğrula
                        .build()
                        .parseClaimsJws(token)
                        .getBody();

                // 4. Token'dan kullanıcı bilgilerini (örneğin ID) al
                String userId = claims.getSubject(); // Genellikle 'subject' alanı kullanıcı ID'si için kullanılır
                if (userId == null) {
                    throw new RuntimeException("Token içinde kullanıcı ID (subject) bulunamadı.");
                }

                // 5. İsteği modifiye ederek downstream servislere ek bilgi gönder
                // Bu sayede arkadaki servisler, isteği yapan kullanıcının kim olduğunu bilir.
                ServerHttpRequest modifiedRequest = exchange.getRequest().mutate()
                        .header("X-User-Id", userId) // Özel bir başlık ekliyoruz
                        .build();

                logger.info("Token doğrulandı. Kullanıcı ID: {}. İstek devam ediyor.", userId);

                // İsteğin, modifiye edilmiş haliyle zincirdeki bir sonraki filtreye devam etmesini sağla
                return chain.filter(exchange.mutate().request(modifiedRequest).build());

            } catch (Exception e) {
                // Token doğrulama sırasında herhangi bir hata olursa (süre dolması, imza hatası vb.)
                logger.error("Token doğrulama hatası: {}", e.getMessage());
                return this.onError(exchange, "Geçersiz veya süresi dolmuş token", HttpStatus.UNAUTHORIZED);
            }
        }

        // Eğer istek halka açık bir yola yapılıyorsa, kontrol yapmadan devam et
        logger.info("Herkese açık endpoint'e istek geldi, devam ediliyor: {}", request.getURI().getPath());
        return chain.filter(exchange);
    }

    /**
     * Hata durumunda istemciye uygun HTTP durum kodu ile boş bir yanıt dönen yardımcı metot.
     */
    private Mono<Void> onError(ServerWebExchange exchange, String errMessage, HttpStatus httpStatus) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(httpStatus);
        // Hata yanıtını logluyoruz
        logger.warn("İstek reddedildi. Sebep: {}, HTTP Status: {}", errMessage, httpStatus);
        return response.setComplete();
    }

    /**
     * Filtrenin çalışma sırasını belirler.
     * Düşük değer, daha yüksek öncelik anlamına gelir.
     * Gateway'in kendi yönlendirme filtrelerinden önce çalışması için 0 veya 1 gibi bir değer verilir.
     */
    @Override
    public int getOrder() {
        return 0;
    }
}