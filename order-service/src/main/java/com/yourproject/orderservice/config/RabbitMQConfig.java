package com.yourproject.orderservice.config;

import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    // Reads the exchange name from your application.properties file
    @Value("${rabbitmq.exchange.name}")
    private String exchangeName;

    // This bean defines the Topic Exchange.
    // A Topic Exchange is powerful because it allows flexible routing of messages.
    @Bean
    public TopicExchange exchange() {
        return new TopicExchange(exchangeName);
    }

    // This bean tells Spring AMQP to use JSON for sending/receiving messages.
    // It will automatically convert your Java event objects to JSON format.
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}