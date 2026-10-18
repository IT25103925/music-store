package com.sliit.musicstore.service.impl;

import com.sliit.musicstore.entity.*;
import com.sliit.musicstore.repository.OrderRepository;
import com.sliit.musicstore.repository.ProductRepository;
import com.sliit.musicstore.service.CartService;
import com.sliit.musicstore.service.NotificationService;
import com.sliit.musicstore.service.OfferService;
import com.sliit.musicstore.service.OrderService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Member 2 - Order Management module (Checkout / Order part).
 */
@Service
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final CartService cartService;
    private final OfferService offerService;
    private final NotificationService notificationService;
    private final com.sliit.musicstore.repository.PaymentRepository paymentRepository;

    public OrderServiceImpl(OrderRepository orderRepository, ProductRepository productRepository,
            CartService cartService, OfferService offerService,
            NotificationService notificationService,
            com.sliit.musicstore.repository.PaymentRepository paymentRepository) {
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
        this.cartService = cartService;
        this.offerService = offerService;
        this.notificationService = notificationService;
        this.paymentRepository = paymentRepository;
    }

    @Override
    @Transactional
    public Order checkout(User user, String shippingAddress, String contactPhone) {
        return checkout(user, user != null ? user.getFullName() : null, shippingAddress, null, null, contactPhone,
                null);
    }

    @Override
    @Transactional
    public Order checkout(User user, String shippingAddress, String contactPhone, String orderNotes) {
        return checkout(user, user != null ? user.getFullName() : null, shippingAddress, null, null, contactPhone,
                orderNotes);
    }

    @Override
    @Transactional
    public Order checkout(User user, String recipientName, String shippingAddress, String city, String postalCode,
            String contactPhone, String orderNotes) {
        Cart cart = cartService.getOrCreateCart(user);
        List<CartItem> activeItems = cart.getItems().stream()
                .filter(item -> !item.isSavedForLater())
                .collect(java.util.stream.Collectors.toList());

        if (activeItems.isEmpty()) {
            throw new IllegalStateException("Cannot checkout with an empty cart");
        }

        Order order = new Order();
        order.setUser(user);
        order.setRecipientName(recipientName);
        order.setShippingAddress(shippingAddress);
        order.setCity(city);
        order.setPostalCode(postalCode);
        order.setContactPhone(contactPhone);
        order.setOrderNotes(orderNotes);

        BigDecimal subtotal = BigDecimal.ZERO;
        for (CartItem cartItem : activeItems) {
            Product product = cartItem.getProduct();
            if (product.getStockQuantity() != null && product.getStockQuantity() < cartItem.getQuantity()) {
                throw new IllegalStateException("Not enough stock for " + product.getName());
            }
            OrderItem orderItem = new OrderItem();
            orderItem.setOrder(order);
            orderItem.setProduct(product);
            orderItem.setQuantity(cartItem.getQuantity());
            orderItem.setUnitPrice(product.getPrice());
            order.getItems().add(orderItem);
            subtotal = subtotal.add(orderItem.getSubtotal());

            // reduce stock immediately (Member 1's product module)
            product.setStockQuantity(product.getStockQuantity() - cartItem.getQuantity());
            productRepository.save(product);
        }
        order.setSubtotal(subtotal);

        BigDecimal discount = BigDecimal.ZERO;
        Optional<Offer> maybeOffer = cart.getAppliedOfferCode() == null
                ? Optional.empty()
                : offerService.findValidByCode(cart.getAppliedOfferCode());
        if (maybeOffer.isPresent()) {
            Offer offer = maybeOffer.get();
            discount = offerService.calculateDiscount(offer, subtotal, activeItems);
            order.setAppliedOfferCode(offer.getCode());
            offerService.recordUsage(offer);
        }
        order.setDiscountAmount(discount);
        order.setTotalAmount(subtotal.subtract(discount));
        order.setStatus(Order.OrderStatus.PENDING);

        Order saved = orderRepository.save(order);

        // Remove active items from cart, retain saved for later items
        cart.getItems().removeIf(item -> !item.isSavedForLater());
        cart.setAppliedOfferCode(null);

        notificationService.notify(user,
                "Your order #" + saved.getId() + " has been placed. Total: LKR " + saved.getTotalAmount(),
                Notification.NotificationType.ORDER, "/orders/" + saved.getId());

        notificationService.notifyAdmins(
                "New order #" + saved.getId() + " placed by " + (user != null ? user.getFullName() : "a customer")
                        + " (Total: LKR " + saved.getTotalAmount() + ")",
                Notification.NotificationType.ORDER, "/admin/orders");

        return saved;
    }

    @Override
    @Transactional
    public Order cancelOrder(Long orderId, User user) {
        Order order = findById(orderId);
        if (order.getUser() != null && !order.getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException("You can only cancel your own orders.");
        }
        Order saved = updateStatus(orderId, Order.OrderStatus.CANCELLED);
        paymentRepository.findByOrderId(orderId).ifPresent(payment -> {
            if (payment.getStatus() == com.sliit.musicstore.entity.Payment.PaymentStatus.SUCCESS) {
                payment.setStatus(com.sliit.musicstore.entity.Payment.PaymentStatus.REFUND_REQUESTED);
                paymentRepository.save(payment);
                notificationService.notifyAdmins(
                        "REFUND REQUESTED: Customer cancelled order #" + order.getId()
                                + ". Payment Officer action required in Payment Management.",
                        Notification.NotificationType.PAYMENT, "/admin/payments");
            }
        });
        return saved;
    }

    @Override
    @Transactional
    public Order requestReturnOrCancel(Long orderId, User user, String reason) {
        Order order = findById(orderId);
        if (order.getUser() != null && !order.getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException("You can only request return or cancellation for your own orders.");
        }
        Order.OrderStatus targetStatus = (order.getStatus() == Order.OrderStatus.DELIVERED)
                ? Order.OrderStatus.RETURN_REQUESTED
                : Order.OrderStatus.CANCEL_REQUESTED;
        order.setStatus(targetStatus);
        if (reason != null && !reason.isBlank()) {
            String updatedNotes = (order.getOrderNotes() == null ? "" : order.getOrderNotes() + "\n")
                    + "[Request Reason: " + reason.trim() + "]";
            order.setOrderNotes(updatedNotes);
        }
        Order saved = orderRepository.save(order);

        // Flag payment for refund in Member 3 Payment Management module
        paymentRepository.findByOrderId(orderId).ifPresent(payment -> {
            if (payment.getStatus() == com.sliit.musicstore.entity.Payment.PaymentStatus.SUCCESS) {
                payment.setStatus(com.sliit.musicstore.entity.Payment.PaymentStatus.REFUND_REQUESTED);
                if (reason != null && !reason.isBlank()) {
                    payment.setNotes("Customer Cancellation Reason: " + reason.trim());
                }
                paymentRepository.save(payment);
                notificationService.notifyAdmins(
                        "REFUND REQUESTED: Customer requested refund of LKR " + payment.getAmount() + " for order #"
                                + order.getId() + ". Reason: " + (reason != null ? reason : "N/A"),
                        Notification.NotificationType.PAYMENT, "/admin/payments");
            }
        });

        notificationService.notify(user,
                "Your " + (targetStatus == Order.OrderStatus.RETURN_REQUESTED ? "return" : "cancellation")
                        + " request for order #" + order.getId() + " has been submitted to store admin.",
                Notification.NotificationType.ORDER, "/orders/" + order.getId());
        notificationService.notifyAdmins(
                "Customer requested " + targetStatus + " for order #" + order.getId() + ". Reason: "
                        + (reason != null ? reason : "N/A"),
                Notification.NotificationType.ORDER, "/admin/orders");
        return saved;
    }

    @Override
    @Transactional
    public Order updateShippingDetails(Long orderId, User user, String recipientName, String shippingAddress,
            String city, String postalCode, String contactPhone, String orderNotes) {
        Order order = findById(orderId);
        if (order.getUser() != null && !order.getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException("You can only edit shipping details for your own orders.");
        }
        if (order.getStatus() != Order.OrderStatus.PENDING) {
            throw new IllegalStateException("Shipping details can only be edited while the order is PENDING.");
        }
        if (recipientName != null && !recipientName.isBlank())
            order.setRecipientName(recipientName.trim());
        if (shippingAddress != null && !shippingAddress.isBlank())
            order.setShippingAddress(shippingAddress.trim());
        if (city != null && !city.isBlank())
            order.setCity(city.trim());
        if (postalCode != null && !postalCode.isBlank())
            order.setPostalCode(postalCode.trim());
        if (contactPhone != null && !contactPhone.isBlank())
            order.setContactPhone(contactPhone.trim());
        if (orderNotes != null && !orderNotes.isBlank())
            order.setOrderNotes(orderNotes.trim());
        return orderRepository.save(order);
    }

    @Override
    @Transactional
    public Order updateTrackingNumber(Long orderId, String trackingNumber) {
        Order order = findById(orderId);
        order.setTrackingNumber(trackingNumber);
        if (order.getStatus() == Order.OrderStatus.CONFIRMED || order.getStatus() == Order.OrderStatus.PENDING) {
            order.setStatus(Order.OrderStatus.SHIPPED);
        }
        Order saved = orderRepository.save(order);
        notificationService.notify(order.getUser(),
                "Your order #" + order.getId() + " tracking number has been updated: " + trackingNumber,
                Notification.NotificationType.ORDER, "/orders/" + order.getId());
        return saved;
    }

    @Override
    public List<Order> findForUser(Long userId) {
        return orderRepository.findByUserIdOrderByOrderDateDesc(userId);
    }

    @Override
    public List<Order> findAllForAdmin() {
        return orderRepository.findAllByOrderByOrderDateDesc();
    }

    @Override
    public Order findById(Long id) {
        return orderRepository.findByIdWithItems(id)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + id));
    }

    @Override
    @Transactional
    public Order updateStatus(Long orderId, Order.OrderStatus status) {
        Order order = findById(orderId);
        Order.OrderStatus oldStatus = order.getStatus();
        order.setStatus(status);
        Order saved = orderRepository.save(order);

        // If order was CANCELLED/rejected:
        if (status == Order.OrderStatus.CANCELLED && oldStatus != Order.OrderStatus.CANCELLED) {
            // 1. Restock item quantities
            if (order.getItems() != null) {
                for (OrderItem item : order.getItems()) {
                    Product p = item.getProduct();
                    if (p != null) {
                        int currentStock = p.getStockQuantity() != null ? p.getStockQuantity() : 0;
                        p.setStockQuantity(currentStock + item.getQuantity());
                        productRepository.save(p);
                    }
                }
            }

            // 2. Mark payment as REFUND_REQUESTED for Payment Manager to process in Member
            // 3 module
            paymentRepository.findByOrderId(orderId).ifPresent(payment -> {
                if (payment.getStatus() == com.sliit.musicstore.entity.Payment.PaymentStatus.SUCCESS) {
                    payment.setStatus(com.sliit.musicstore.entity.Payment.PaymentStatus.REFUND_REQUESTED);
                    paymentRepository.save(payment);
                    notificationService.notify(order.getUser(),
                            "Refund request of LKR " + payment.getAmount() + " for cancelled order #" + order.getId()
                                    + " has been forwarded to our Payment Department.",
                            Notification.NotificationType.PAYMENT, "/orders/" + order.getId());
                    notificationService.notifyAdmins(
                            "REFUND REQUIRED: Order #" + order.getId() + " was cancelled. Payment of LKR "
                                    + payment.getAmount() + " requires Payment Officer refund in Payment Management.",
                            Notification.NotificationType.PAYMENT, "/admin/payments");
                }
            });
        }

        notificationService.notify(order.getUser(),
                "Your order #" + order.getId() + " is now " + status,
                Notification.NotificationType.ORDER, "/orders/" + order.getId());
        return saved;
    }
}
