package com.ecommerce.order.service;

import com.ecommerce.order.client.ProductServiceClient;
import com.ecommerce.order.client.UserServiceClient;
import com.ecommerce.order.domain.Order;
import com.ecommerce.order.domain.OrderItem;
import com.ecommerce.order.domain.OrderStatus;
import com.ecommerce.order.dto.ProductDto;
import com.ecommerce.order.dto.UserDto;
import com.ecommerce.order.repository.OrderRepository;
// import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
// import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderService {

    private final OrderRepository orderRepository;
    private final UserServiceClient userServiceClient;
    private final ProductServiceClient productServiceClient;

    @Transactional
    // @CircuitBreaker(name = "order-service")
    // @Retry(name = "order-service")
    public Order createOrder(Long userId, List<OrderItem> orderItems) {

        // 1. 사용자 정보 확인
        UserDto user = userServiceClient.getUser(userId);
        if (user == null || user.fallback()) {
            throw new RuntimeException("사용자 서비스를 사용할 수 없습니다.");
        }

        // 주문 생성
        Order order = new Order();

        // 2. 상품 정보 확인
        for (OrderItem orderItem : orderItems) {
            ProductDto product = productServiceClient.getProduct(orderItem.getProductId());
            if (product == null || product.fallback()) {
                throw new RuntimeException("상품 서비스를 사용할 수 없습니다");
            }

            // 재고 확인
            if (product.stockQuantity() < orderItem.getQuantity()) {
                throw new RuntimeException("재고가 부족합니다");
            }

            Boolean stockDecreased = productServiceClient.decreaseStock(orderItem.getProductId(), orderItem.getQuantity());
            if (!stockDecreased) {
                throw new RuntimeException("재고 차감에 실패했습니다");
            }

            orderItem.setUnitPrice(product.price());
            order.addOrderItem(orderItem);
        }

        order.setUserId(userId);
        order.setUserName(user.name());
        order.setUserEmail(user.email());
        // 총 금액 계산
        order.calculateTotalAmount();

        return orderRepository.save(order);
    }

    public Optional<Order> findById(Long id) {
        return orderRepository.findById(id);
    }

    public List<Order> findAllOrders() {
        return orderRepository.findAll();
    }

    public List<Order> findByUserId(Long userId) {
        return orderRepository.findByUserId(userId);
    }

    public List<Order> findByStatus(OrderStatus status) {
        return orderRepository.findByStatus(status);
    }

    @Transactional
    public Order updateOrderStatus(Long id, OrderStatus status) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("주문을 찾을 수 없습니다: " + id));

        order.setStatus(status);
        return orderRepository.save(order);
    }

    @Transactional
    public void cancelOrder(Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("주문을 찾을 수 없습니다: " + id));

        if (order.getStatus() != OrderStatus.PENDING) {
            throw new RuntimeException("진행 중인 주문만 취소할 수 있습니다.");
        }

        order.setStatus(OrderStatus.CANCELLED);
        orderRepository.save(order);

        // 재고 원복 (실제로는 별도 재고 관리 로직이 필요)
        for (OrderItem item : order.getOrderItems()) {
            ProductDto product = productServiceClient.getProduct(item.getProductId());
            if (product != null) {
                // 재고 증가 로직
                // product.setStockQuantity(product.getStockQuantity() + item.getQuantity());
            }
        }
    }

}