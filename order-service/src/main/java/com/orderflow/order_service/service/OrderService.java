package com.orderflow.order_service.service;

import com.orderflow.order_service.model.Order;
import com.orderflow.order_service.repository.OrderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.client.HttpClientErrorException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
public class OrderService {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private RestTemplate restTemplate;

    private static final String PRODUCT_SERVICE_URL = "http://localhost:8082/api/products";

    public Order placeOrder(Order order) {
        // Step 1: Fetch the product from Product Service
        Map<String, Object> product;
        try {
            product = restTemplate.getForObject(
                    PRODUCT_SERVICE_URL + "/" + order.getProductId(),
                    Map.class
            );
        } catch (HttpClientErrorException.NotFound e) {
            throw new RuntimeException("Product not found with id: " + order.getProductId());
        }

        if (product == null) {
            throw new RuntimeException("Product not found with id: " + order.getProductId());
        }

        // Step 2: Check stock
        Integer stockQuantity = (Integer) product.get("stockQuantity");
        if (stockQuantity == null || stockQuantity < order.getQuantity()) {
            order.setStatus("FAILED");
            order.setCreatedAt(LocalDateTime.now());
            order.setUpdatedAt(LocalDateTime.now());
            orderRepository.save(order);
            throw new RuntimeException("Insufficient stock for product id: " + order.getProductId());
        }

        // Step 3: Calculate total price
        Double price = ((Number) product.get("price")).doubleValue();
        BigDecimal totalPrice = BigDecimal.valueOf(price).multiply(BigDecimal.valueOf(order.getQuantity()));
        order.setTotalPrice(totalPrice);

        // Step 4: Reduce stock in Product Service
        restTemplate.exchange(
                PRODUCT_SERVICE_URL + "/" + order.getProductId() + "/reduce-stock?quantity=" + order.getQuantity(),
                org.springframework.http.HttpMethod.PATCH,
                null,
                Map.class
        );

        // Step 5: Save the confirmed order
        order.setStatus("CONFIRMED");
        order.setCreatedAt(LocalDateTime.now());
        order.setUpdatedAt(LocalDateTime.now());
        return orderRepository.save(order);
    }

    public List<Order> getAllOrders() {
        return orderRepository.findAll();
    }

    public Order getOrderById(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Order not found with id: " + id));
    }
}
