package com.orderflow.order_service.service;

import com.orderflow.order_service.config.RabbitMQConfig;
import com.orderflow.order_service.dto.OrderPlacedEvent;
import com.orderflow.order_service.model.Order;
import com.orderflow.order_service.repository.OrderRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class OrderService {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @CircuitBreaker(name = "productService", fallbackMethod = "fallbackCreateOrder")
    public Order createOrder(Order order) {
        order.setStatus("CONFIRMED");
        Order savedOrder = orderRepository.save(order);

        try {
            OrderPlacedEvent event = new OrderPlacedEvent(
                savedOrder.getId(),
                1L,
                savedOrder.getProductId(),
                savedOrder.getQuantity(),
                savedOrder.getTotalPrice(),
                savedOrder.getStatus()
            );

            rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE, RabbitMQConfig.ROUTING_KEY, event);
            System.out.println(">>> [ORDER SERVICE] Published OrderPlacedEvent for Order ID: " + savedOrder.getId());
        } catch (Exception e) {
            System.err.println(">>> [ORDER SERVICE] RabbitMQ publish warning: " + e.getMessage());
        }

        return savedOrder;
    }

    public Order fallbackCreateOrder(Order order, Throwable throwable) {
        System.err.println(">>> [CIRCUIT BREAKER] Fallback triggered: " + throwable.getMessage());
        order.setStatus("PENDING_FALLBACK");
        if (order.getTotalPrice() == null) {
            order.setTotalPrice(BigDecimal.ZERO);
        }
        return orderRepository.save(order);
    }

    public List<Order> getAllOrders() {
        return orderRepository.findAll();
    }
}