package com.ecommerce.order.client;

import com.ecommerce.order.dto.ProductDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.net.ConnectException;
import java.net.SocketTimeoutException;

@Slf4j
@Component
public class ProductServiceFallbackFactory implements FallbackFactory<ProductServiceClient> {

    @Override
    public ProductServiceClient create(Throwable cause) {
        return new ProductServiceClient() {
            @Override
            public ProductDto getProduct(Long id) {
                logError("getProduct", id, cause);
                return createFallbackProduct(id);
            }

            @Override
            public Boolean decreaseStock(Long productId, Integer quantity) {
                logError("decreaseStock", productId, cause);
                return false;
            }
        };
    }

    private void logError(String operation, Long id, Throwable cause) {
        if (cause instanceof SocketTimeoutException) {
            log.warn("[PRODUCT-SERVICE] Timeout occurred during {} for product {}: {}", operation, id, cause.getMessage());
        } else if (cause instanceof ConnectException) {
            log.error("[PRODUCT-SERVICE] Connection failed during {} for product {}: {}", operation, id, cause.getMessage());
        } else {
            log.error("[PRODUCT-SERVICE] Unexpected error during {} for product {}: {}", operation, id, cause.getMessage());
        }
    }

    public ProductDto createFallbackProduct(Long id) {
        return new ProductDto(id, "Unknown Product", BigDecimal.ZERO, 0, true);
    }

}
