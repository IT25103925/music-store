package com.sliit.musicstore.controller;

import com.sliit.musicstore.entity.Order;
import com.sliit.musicstore.service.OrderService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

/** Member 2 - Order Management module (admin order processing). */
@Controller
@RequestMapping("/admin/orders")
public class AdminOrderController {

    private final OrderService orderService;

    public AdminOrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping
    public String list(Model model) {
        var orders = orderService.findAllForAdmin();
        
        java.math.BigDecimal totalRevenue = orders.stream()
                .filter(o -> o.getStatus() != Order.OrderStatus.CANCELLED)
                .map(Order::getTotalAmount)
                .filter(java.util.Objects::nonNull)
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);

        long pendingCount = orders.stream().filter(o -> o.getStatus() == Order.OrderStatus.PENDING).count();
        long confirmedCount = orders.stream().filter(o -> o.getStatus() == Order.OrderStatus.CONFIRMED).count();
        long shippedCount = orders.stream().filter(o -> o.getStatus() == Order.OrderStatus.SHIPPED).count();
        long deliveredCount = orders.stream().filter(o -> o.getStatus() == Order.OrderStatus.DELIVERED).count();
        
        java.math.BigDecimal avgOrderValue = (orders.isEmpty() || totalRevenue.compareTo(java.math.BigDecimal.ZERO) == 0) 
                ? java.math.BigDecimal.ZERO 
                : totalRevenue.divide(java.math.BigDecimal.valueOf(Math.max(1, orders.size() - orders.stream().filter(o -> o.getStatus() == Order.OrderStatus.CANCELLED).count())), 2, java.math.RoundingMode.HALF_UP);

        model.addAttribute("orders", orders);
        model.addAttribute("totalRevenue", totalRevenue);
        model.addAttribute("totalOrdersCount", orders.size());
        model.addAttribute("pendingCount", pendingCount);
        model.addAttribute("confirmedCount", confirmedCount);
        model.addAttribute("shippedCount", shippedCount);
        model.addAttribute("deliveredCount", deliveredCount);
        model.addAttribute("avgOrderValue", avgOrderValue);
        
        return "admin/orders";
    }

    @PostMapping("/{id}/status")
    public String updateStatus(@PathVariable Long id, @RequestParam Order.OrderStatus status) {
        orderService.updateStatus(id, status);
        return "redirect:/admin/orders";
    }

    @PostMapping("/{id}/tracking")
    public String updateTracking(@PathVariable Long id, @RequestParam String trackingNumber) {
        orderService.updateTrackingNumber(id, trackingNumber);
        return "redirect:/admin/orders";
    }

    @GetMapping("/{id}/shipping-label")
    public String shippingLabel(@PathVariable Long id, Model model) {
        model.addAttribute("order", orderService.findById(id));
        return "admin/shipping-label";
    }

    @GetMapping("/export-csv")
    public void exportCsv(jakarta.servlet.http.HttpServletResponse response) throws java.io.IOException {
        response.setContentType("text/csv");
        response.setHeader("Content-Disposition", "attachment; filename=\"melodia_orders_report_" + System.currentTimeMillis() + ".csv\"");

        var orders = orderService.findAllForAdmin();
        var writer = response.getWriter();
        
        writer.println("Order ID,Customer Name,Recipient Name,Customer Email,Order Date,Status,Tracking Number,Shipping Address,City,Postal Code,Contact Phone,Offer Code,Subtotal (LKR),Discount (LKR),Total Amount (LKR),Order Notes");

        for (Order o : orders) {
            String id = "#ORD-" + String.format("%04d", o.getId());
            String name = escapeCsv(o.getUser() != null ? o.getUser().getFullName() : "Guest");
            String recipient = escapeCsv(o.getRecipientName() != null ? o.getRecipientName() : (o.getUser() != null ? o.getUser().getFullName() : ""));
            String email = escapeCsv(o.getUser() != null ? o.getUser().getEmail() : "");
            String date = o.getOrderDate() != null ? o.getOrderDate().toString() : "";
            String status = o.getStatus() != null ? o.getStatus().name() : "";
            String tracking = escapeCsv(o.getTrackingNumber() != null ? o.getTrackingNumber() : "");
            String address = escapeCsv(o.getShippingAddress() != null ? o.getShippingAddress() : "");
            String city = escapeCsv(o.getCity() != null ? o.getCity() : "");
            String postal = escapeCsv(o.getPostalCode() != null ? o.getPostalCode() : "");
            String phone = escapeCsv(o.getContactPhone() != null ? o.getContactPhone() : "");
            String offer = escapeCsv(o.getAppliedOfferCode() != null ? o.getAppliedOfferCode() : "");
            String subtotal = o.getSubtotal() != null ? o.getSubtotal().toString() : "0.00";
            String discount = o.getDiscountAmount() != null ? o.getDiscountAmount().toString() : "0.00";
            String total = o.getTotalAmount() != null ? o.getTotalAmount().toString() : "0.00";
            String notes = escapeCsv(o.getOrderNotes() != null ? o.getOrderNotes() : "");

            writer.println(String.format("%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s",
                    id, name, recipient, email, date, status, tracking, address, city, postal, phone, offer, subtotal, discount, total, notes));
        }
        writer.flush();
    }

    private String escapeCsv(String data) {
        if (data == null) return "";
        String escaped = data.replaceAll("\"", "\"\"");
        if (escaped.contains(",") || escaped.contains("\n") || escaped.contains("\"")) {
            escaped = "\"" + escaped + "\"";
        }
        return escaped;
    }
}
