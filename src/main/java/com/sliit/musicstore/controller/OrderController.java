package com.sliit.musicstore.controller;

import com.sliit.musicstore.entity.Order;
import com.sliit.musicstore.entity.Payment;
import com.sliit.musicstore.entity.User;
import com.sliit.musicstore.service.OrderService;
import com.sliit.musicstore.service.PaymentService;
import com.sliit.musicstore.service.UserService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.time.LocalDateTime;
import java.util.List;

/** Member 2 - Order Management module (Checkout / order history). */
@Controller
@RequestMapping("/orders")
public class OrderController {

    private final OrderService orderService;
    private final UserService userService;
    private final PaymentService paymentService;
    private final com.sliit.musicstore.service.CartService cartService;
    private final com.sliit.musicstore.service.OfferService offerService;

    public OrderController(OrderService orderService, UserService userService, PaymentService paymentService,
                           com.sliit.musicstore.service.CartService cartService,
                           com.sliit.musicstore.service.OfferService offerService) {
        this.orderService = orderService;
        this.userService = userService;
        this.paymentService = paymentService;
        this.cartService = cartService;
        this.offerService = offerService;
    }

    @GetMapping("/checkout")
    public String checkoutPage(Model model, Principal principal) {
        User user = userService.getCurrentUser(principal);
        com.sliit.musicstore.entity.Cart cart = cartService.getOrCreateCart(user);

        java.util.List<com.sliit.musicstore.entity.CartItem> activeItems = cart.getItems().stream()
                .filter(item -> !item.isSavedForLater())
                .collect(java.util.stream.Collectors.toList());

        java.math.BigDecimal subtotal = activeItems.stream()
                .map(com.sliit.musicstore.entity.CartItem::getSubtotal)
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);

        java.math.BigDecimal discount = java.math.BigDecimal.ZERO;
        if (cart.getAppliedOfferCode() != null) {
            java.util.Optional<com.sliit.musicstore.entity.Offer> offer = offerService.findValidByCode(cart.getAppliedOfferCode());
            if (offer.isPresent()) {
                discount = offerService.calculateDiscount(offer.get(), subtotal, activeItems);
            }
        }

        model.addAttribute("user", user);
        model.addAttribute("cart", cart);
        model.addAttribute("activeItems", activeItems);
        model.addAttribute("subtotal", subtotal);
        model.addAttribute("discount", discount);
        model.addAttribute("total", subtotal.subtract(discount));
        return "order/checkout";
    }

    @PostMapping("/checkout")
    public String checkout(@RequestParam(required = false) String recipientName,
                            @RequestParam(required = false) String shippingAddress,
                            @RequestParam(required = false) String city,
                            @RequestParam(required = false) String postalCode,
                            @RequestParam(required = false) String contactPhone,
                            @RequestParam(required = false) String orderNotes,
                            @RequestParam(required = false, defaultValue = "CREDIT_CARD") String paymentMethod,
                            @RequestParam(required = false) String cardNumber,
                            Principal principal,
                            RedirectAttributes redirectAttributes) {
        User user = userService.getCurrentUser(principal);
        Order order = null;
        try {
            String name = (recipientName != null && !recipientName.trim().isEmpty()) ? recipientName : (user != null ? user.getFullName() : "Customer");
            String address = (shippingAddress != null && !shippingAddress.trim().isEmpty()) ? shippingAddress : (user != null ? user.getAddress() : "Address N/A");
            String phone = (contactPhone != null && !contactPhone.trim().isEmpty()) ? contactPhone : (user != null ? user.getPhone() : "Phone N/A");

            order = orderService.checkout(user, name, address, city, postalCode, phone, orderNotes);
        } catch (IllegalStateException ex) {
            // Check if user has an order created in the last 2 minutes (prevents double-submit or empty-cart errors)
            if (user != null) {
                List<Order> userOrders = orderService.findForUser(user.getId());
                if (userOrders != null && !userOrders.isEmpty()) {
                    Order recentOrder = userOrders.get(0);
                    if (recentOrder.getOrderDate() != null && 
                        recentOrder.getOrderDate().isAfter(LocalDateTime.now().minusMinutes(2))) {
                        order = recentOrder;
                    }
                }
            }
            if (order == null) {
                redirectAttributes.addFlashAttribute("checkoutError", ex.getMessage());
                return "redirect:/cart";
            }
        }

        // Always redirect to dedicated payment selection page (/payment/pay) where user selects Card vs COD
        return "redirect:/payment/pay?orderId=" + order.getId();
    }

    @PostMapping("/{id}/cancel")
    public String cancelOrder(@PathVariable Long id, Principal principal, RedirectAttributes redirectAttributes) {
        User user = userService.getCurrentUser(principal);
        try {
            orderService.cancelOrder(id, user);
            redirectAttributes.addFlashAttribute("orderSuccess", "Order #" + id + " has been successfully cancelled and item stock has been restored.");
            redirectAttributes.addFlashAttribute("success", "Order #" + id + " has been successfully cancelled and item stock has been restored.");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("orderError", ex.getMessage());
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/orders";
    }

    @PostMapping("/{id}/request-cancel-return")
    public String requestCancelReturn(@PathVariable Long id, @RequestParam(required = false) String reason, Principal principal, RedirectAttributes redirectAttributes) {
        User user = userService.getCurrentUser(principal);
        try {
            orderService.requestReturnOrCancel(id, user, reason);
            redirectAttributes.addFlashAttribute("orderSuccess", "Your cancellation/return request for Order #" + id + " has been submitted to customer support.");
            redirectAttributes.addFlashAttribute("success", "Your cancellation/return request for Order #" + id + " has been submitted to customer support.");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("orderError", ex.getMessage());
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/orders/" + id;
    }

    @PostMapping("/{id}/update-address")
    public String updateAddress(@PathVariable Long id,
                                @RequestParam(required = false) String recipientName,
                                @RequestParam(required = false) String shippingAddress,
                                @RequestParam(required = false) String city,
                                @RequestParam(required = false) String postalCode,
                                @RequestParam(required = false) String contactPhone,
                                @RequestParam(required = false) String orderNotes,
                                Principal principal,
                                RedirectAttributes redirectAttributes) {
        User user = userService.getCurrentUser(principal);
        try {
            orderService.updateShippingDetails(id, user, recipientName, shippingAddress, city, postalCode, contactPhone, orderNotes);
            redirectAttributes.addFlashAttribute("orderSuccess", "Shipping details for Order #" + id + " updated successfully.");
            redirectAttributes.addFlashAttribute("success", "Shipping details for Order #" + id + " updated successfully.");
        } catch (Exception ex) {
            redirectAttributes.addFlashAttribute("orderError", ex.getMessage());
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/orders/" + id;
    }

    @GetMapping
    public String myOrders(Model model, Principal principal) {
        User user = userService.getCurrentUser(principal);
        if (user != null) {
            model.addAttribute("orders", orderService.findForUser(user.getId()));
        }
        return "order/my-orders";
    }

    @GetMapping("/{id}")
    public String orderDetail(@PathVariable Long id, Model model, Principal principal, RedirectAttributes redirectAttributes) {
        User user = userService.getCurrentUser(principal);
        Order order = orderService.findById(id);
        if (!isAuthorizedToAccessOrder(user, order)) {
            redirectAttributes.addFlashAttribute("orderError", "You are not authorized to view Order #" + id);
            return "redirect:/orders";
        }
        Payment payment = paymentService.findByOrderId(id);
        model.addAttribute("order", order);
        model.addAttribute("payment", payment);
        return "order/order-detail";
    }

    @GetMapping("/{id}/invoice")
    public String viewInvoice(@PathVariable Long id, Model model, Principal principal, RedirectAttributes redirectAttributes) {
        User user = userService.getCurrentUser(principal);
        Order order = orderService.findById(id);
        if (!isAuthorizedToAccessOrder(user, order)) {
            redirectAttributes.addFlashAttribute("orderError", "You are not authorized to view the invoice for Order #" + id);
            return "redirect:/orders";
        }
        model.addAttribute("order", order);
        return "order/invoice";
    }

    @PostMapping("/{id}/reorder")
    public String reorder(@PathVariable Long id, Principal principal, RedirectAttributes redirectAttributes) {
        User user = userService.getCurrentUser(principal);
        Order order = orderService.findById(id);
        if (!isAuthorizedToAccessOrder(user, order)) {
            redirectAttributes.addFlashAttribute("orderError", "You are not authorized to reorder items from Order #" + id);
            return "redirect:/orders";
        }
        if (order != null && order.getItems() != null) {
            int addedCount = 0;
            for (com.sliit.musicstore.entity.OrderItem item : order.getItems()) {
                if (item.getProduct() != null) {
                    cartService.addItem(user, item.getProduct().getId(), item.getQuantity());
                    addedCount++;
                }
            }
            redirectAttributes.addFlashAttribute("success", "Added " + addedCount + " item(s) from Order #ORD-" + String.format("%04d", order.getId()) + " back into your cart!");
        }
        return "redirect:/cart";
    }

    private boolean isAuthorizedToAccessOrder(User user, Order order) {
        if (user == null || order == null) return false;
        if (user.getRole() != null && "ADMIN".equalsIgnoreCase(user.getRole().name())) return true;
        return order.getUser() != null && user.getId().equals(order.getUser().getId());
    }

    @GetMapping({"/{id}/cancel", "/{id}/request-cancel-return", "/{id}/update-address", "/{id}/reorder"})
    public String fallbackOrderActions(@PathVariable Long id) {
        return "redirect:/orders/" + id;
    }

    @PostMapping("/{id}")
    public String fallbackPostOrder(@PathVariable Long id) {
        return "redirect:/orders/" + id;
    }
}
