package com.orderflow.order_service.consumer;

import com.orderflow.order_service.config.RabbitMQConfig;
import com.orderflow.order_service.dto.OrderPlacedEvent;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

@Service
public class OrderPlacedConsumer {

    @RabbitListener(queues = RabbitMQConfig.QUEUE)
    public void handleOrderPlaced(OrderPlacedEvent event) {
        System.out.println("=============================================");
        System.out.println(">>> [NOTIFICATION SERVICE] Order Notification Received!");
        System.out.println(">>> Order ID: " + event.getOrderId());
        System.out.println(">>> Product ID: " + event.getProductId());
        System.out.println(">>> Quantity: " + event.getQuantity());
        System.out.println(">>> Total Price: " + event.getTotalPrice());
        System.out.println(">>> Status: " + event.getStatus());
        System.out.println("=============================================");
    }
}