package com.ecommerce.order.dto;

import java.math.BigDecimal;

public record ProductDto(Long id, String name, BigDecimal price, int stockQuantity, boolean fallback) {
}
