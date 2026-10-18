package com.sliit.musicstore.service;

import com.sliit.musicstore.entity.Order;
import com.sliit.musicstore.entity.User;

import java.util.List;

public interface OrderService {
    Order checkout(User user, String recipientName, String shippingAddress, String city, String postalCode, String contactPhone, String orderNotes);
    Order checkout(User user, String shippingAddress, String contactPhone, String orderNotes);
    Order checkout(User user, String shippingAddress, String contactPhone);
    List<Order> findForUser(Long userId);
    List<Order> findAllForAdmin();
    Order findById(Long id);
    Order updateStatus(Long orderId, Order.OrderStatus status);
    Order cancelOrder(Long orderId, User user);
    Order requestReturnOrCancel(Long orderId, User user, String reason);
    Order updateShippingDetails(Long orderId, User user, String recipientName, String shippingAddress, String city, String postalCode, String contactPhone, String orderNotes);
    Order updateTrackingNumber(Long orderId, String trackingNumber);
}
