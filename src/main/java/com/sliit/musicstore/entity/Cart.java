package com.sliit.musicstore.entity;

import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Member 2 - Order Management module (Cart part).
 * Each customer has exactly one cart that persists between visits.
 */
@Entity
@Table(name = "carts")
public class Cart {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", unique = true)
    private User user;

    @OneToMany(mappedBy = "cart", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CartItem> items = new ArrayList<>();

    /** Coupon code currently applied to this cart, if any (Member 5's module) */
    private String appliedOfferCode;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public List<CartItem> getItems() { return items; }
    public void setItems(List<CartItem> items) { this.items = items; }
    public String getAppliedOfferCode() { return appliedOfferCode; }
    public void setAppliedOfferCode(String appliedOfferCode) { this.appliedOfferCode = appliedOfferCode; }

    public java.math.BigDecimal getTotalAmount() {
        if (items == null || items.isEmpty()) {
            return java.math.BigDecimal.ZERO;
        }
        return items.stream()
                .map(CartItem::getSubtotal)
                .filter(java.util.Objects::nonNull)
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
    }
}
