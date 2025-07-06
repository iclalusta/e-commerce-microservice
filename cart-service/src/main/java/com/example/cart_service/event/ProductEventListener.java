package com.example.cart_service.event;

import com.example.cart_service.model.ProductDto;
import com.example.cart_service.service.CartService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ProductEventListener {

    private static final Logger log = LoggerFactory.getLogger(ProductEventListener.class);

    @Autowired
    private CartService cartService;

    // application.properties dosyasında tanımlanan kuyruğu dinler.
    @RabbitListener(queues = "${product.updated.queue}")
    public void onProductUpdated(ProductUpdatedEvent event) {
        if (event == null || event.getProductId() == null) {
            log.warn("Received an incomplete product updated event. Ignoring.");
            return;
        }

        log.info("Product updated event received for product ID: {}", event.getProductId());
        try {
            // Event DTO'sundan CartService'in beklediği ProductDto'ya dönüşüm yapılıyor.
            ProductDto productUpdateDto = new ProductDto();
            productUpdateDto.setId(event.getProductId());

            // --- DÜZELTME BURADA ---
            // Doğru getter metotları kullanılıyor: event.getName() ve event.getPrice()
            productUpdateDto.setName(event.getName());
            productUpdateDto.setPrice(event.getPrice());
            productUpdateDto.setStock(event.getStock());
            // --- DÜZELTME SONU ---


            cartService.updateProductInCarts(event.getProductId(), productUpdateDto); // Servis metodunu çağırıyoruz.

            log.info("Carts updated successfully for product ID: {}", event.getProductId());
        } catch (Exception e) {
            log.error("Error updating carts for product ID: " + event.getProductId(), e);
        }
    }
}