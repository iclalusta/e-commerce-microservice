package com.example.cart_service.config;

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

    @Value("${rabbitmq.queue.name}")
    private String queueName;

    @Value("${rabbitmq.exchange.name}")
    private String exchangeName;

    @Value("${rabbitmq.routing.key}")
    private String routingKey;

    @Bean
    public Queue queue() {
        return new Queue(queueName);
    }

    @Bean
    public DirectExchange exchange() {
        return new DirectExchange(exchangeName);
    }

    @Bean
    public Binding binding(Queue queue, DirectExchange exchange) {
        return BindingBuilder.bind(queue).to(exchange).with(routingKey);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    // --- YENİ ProductUpdatedEvent Ayarları ---
    @Value("${product.updated.queue}")
    private String productQueueName;

    @Value("${rabbitmq.exchange.product}")
    private String productExchangeName;

    @Value("${rabbitmq.routingkey.product.updated}")
    private String productRoutingKey;

    @Bean
    public Queue productUpdatedQueue() {
        return new Queue(productQueueName);
    }

    @Bean
    public DirectExchange productExchange() {
        return new DirectExchange(productExchangeName);
    }

    @Bean
    public Binding productUpdatedBinding(Queue productUpdatedQueue, DirectExchange productExchange) {
        return BindingBuilder.bind(productUpdatedQueue).to(productExchange).with(productRoutingKey);
    }
    // --- YENİ Ayarlar Sonu ---



}