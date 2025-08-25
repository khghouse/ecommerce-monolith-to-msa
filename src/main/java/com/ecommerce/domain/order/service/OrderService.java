package com.ecommerce.domain.order.service;

import com.ecommerce.domain.order.entity.Order;
import com.ecommerce.domain.order.entity.OrderItem;
import com.ecommerce.domain.order.entity.OrderStatus;
import com.ecommerce.domain.order.repository.OrderRepository;
import com.ecommerce.domain.product.entity.Product;
import com.ecommerce.domain.product.service.ProductService;
import com.ecommerce.domain.user.entity.User;
import com.ecommerce.domain.user.service.UserService;
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
    private final ProductService productService;
    private final UserService userService;

    @Transactional
    public Order createOrder(Long userId, List<OrderItem> orderItems) {
        // 사용자 정보 조회
        User user = userService.findById(userId)
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다: " + userId));

        // 주문 생성
        Order order = new Order();
        order.setUserId(userId);
        order.setUserName(user.getName());
        order.setUserEmail(user.getEmail());
        order.setShippingAddress(user.getAddress());

        // 주문 항목 처리
        for (OrderItem orderItem : orderItems) {
            // 상품 정보 조회 및 재고 확인
            Product product = productService.findById(orderItem.getProductId())
                    .orElseThrow(() -> new RuntimeException("상품을 찾을 수 없습니다: " + orderItem.getProductId()));

            if (!product.isAvailable(orderItem.getQuantity())) {
                throw new RuntimeException("재고가 부족합니다. 상품: " + product.getName());
            }

            // 주문 항목 정보 설정
            orderItem.setProductName(product.getName());
            orderItem.setUnitPrice(product.getPrice());
            order.addOrderItem(orderItem);

            // 재고 차감
            productService.decreaseStock(product.getId(), orderItem.getQuantity());
        }

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
            Product product = productService.findById(item.getProductId()).orElse(null);
            if (product != null) {
                product.setStockQuantity(product.getStockQuantity() + item.getQuantity());
            }
        }
    }
}
