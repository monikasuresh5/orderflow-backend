package com.orderflow.order_service.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "orders")
@Data
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Product ID is required")
    private Long productId;

    @NotNull(message = "Quantity is required")
    private Integer quantity;

    private BigDecimal totalPrice;

    private String status; // e.g. "PENDING", "CONFIRMED", "FAILED"

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
} 
