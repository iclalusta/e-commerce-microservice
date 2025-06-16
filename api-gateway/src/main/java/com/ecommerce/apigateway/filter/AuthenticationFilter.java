package com.ecommerce.apigateway.filter;

import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;

@Component
public class AuthenticationFilter extends AbstractGatewayFilterFactory<AuthenticationFilter.Config> {

    private final WebClient.Builder webClientBuilder;

    // Kimlik doğrulaması GEREKTİRMEYEN (herkese açık) endpoint'ler
    private final List<String> openApiEndpoints = List.of(
            "/auth/login",
            "/user/register"
    );

    public AuthenticationFilter(WebClient.Builder webClientBuilder) {
        super(Config.class);
        this.webClientBuilder = webClientBuilder;
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            String path = exchange.getRequest().getURI().getPath();

            // Eğer istek public bir endpoint'e gitmiyorsa, token'ı doğrula
            if (isSecured(path)) {
                if (!exchange.getRequest().getHeaders().containsKey(HttpHeaders.AUTHORIZATION)) {
                    return setErrorResponse(exchange, HttpStatus.UNAUTHORIZED, "Authorization header eksik");
                }

                String authHeader = exchange.getRequest().getHeaders().get(HttpHeaders.AUTHORIZATION).get(0);
                if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                    return setErrorResponse(exchange, HttpStatus.UNAUTHORIZED, "Bearer token formatı hatalı");
                }
                String token = authHeader.substring(7);

                // Docker network'ündeki auth-service'e istek atarak token'ı doğrula
                return webClientBuilder.build().get()
                        .uri("http://auth-service:8081/auth/validate")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .retrieve()
                        .toBodilessEntity()
                        .flatMap(response -> {
                            // auth-service'den 2xx (başarılı) yanıt gelirse
                            if (response.getStatusCode().is2xxSuccessful()) {
                                return chain.filter(exchange); // Token geçerli, isteğin devam etmesine izin ver
                            }
                            // auth-service'den yetkisiz yanıtı gelirse
                            return setErrorResponse(exchange, HttpStatus.UNAUTHORIZED, "Token geçersiz veya süresi dolmuş");
                        }).onErrorResume(e ->
                                // auth-service'e ulaşılamazsa
                                setErrorResponse(exchange, HttpStatus.INTERNAL_SERVER_ERROR, "Kimlik doğrulama servisine ulaşılamıyor")
                        );
            }

            // Public endpoint ise (login, register), kontrol yapmadan devam et
            return chain.filter(exchange);
        };
    }

    private boolean isSecured(String path) {
        // Gelen path, public listesindeki herhangi bir URI'ı içeriyorsa güvenli DEĞİLDİR.
        return openApiEndpoints.stream().noneMatch(path::contains);
    }

    private Mono<Void> setErrorResponse(ServerWebExchange exchange, HttpStatus status, String message) {
        exchange.getResponse().setStatusCode(status);
        exchange.getResponse().getHeaders().add("Content-Type", "application/json; charset=utf-8");
        String errorJson = "{\"status\":\"error\",\"message\":\"" + message + "\"}";
        return exchange.getResponse().writeWith(Mono.just(exchange.getResponse()
                .bufferFactory().wrap(errorJson.getBytes())));
    }

    public static class Config {}
}