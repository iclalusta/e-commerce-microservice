package com.example.productservice.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    // --- 1. OrderCreatedEvent TÜKETİCİ Yapılandırması ---
    @Value("${rabbitmq.queue.stock-decrease}")
    private String stockDecreaseQueueName;

    @Value("${rabbitmq.exchange.order}")
    private String orderExchangeName;

    @Value("${rabbitmq.routingkey.order-created}")
    private String orderCreatedRoutingKey;

    // Stok düşürme işlemleri için dinlenecek kuyruk (Queue)
    @Bean
    public Queue stockDecreaseQueue() {
        return new Queue(stockDecreaseQueueName);
    }

    // Order Service tarafından kullanılan exchange'i burada da tanımlıyoruz ki ona bağlanabilelim.
    @Bean
    public DirectExchange orderExchange() {
        return new DirectExchange(orderExchangeName);
    }

    // Stok düşürme kuyruğunu sipariş exchange'ine bağlama (Binding)
    @Bean
    public Binding stockDecreaseBinding(Queue stockDecreaseQueue, DirectExchange orderExchange) {
        return BindingBuilder.bind(stockDecreaseQueue).to(orderExchange).with(orderCreatedRoutingKey);
    }

    // --- 2. ProductUpdatedEvent ÜRETİCİ Yapılandırması ---
    @Value("${rabbitmq.exchange.product}")
    private String productExchangeName;

    // Bu servis tarafından olay yayınlamak için kullanılacak exchange
    @Bean
    public DirectExchange productExchange() {
        return new DirectExchange(productExchangeName);
    }

    // --- Ortak Yapılandırma ---
    // Tüm olay nesnelerini JSON formatına çevirmek için ortak MessageConverter
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}