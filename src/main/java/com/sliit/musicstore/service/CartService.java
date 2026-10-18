package com.sliit.musicstore.service;

import com.sliit.musicstore.entity.Cart;
import com.sliit.musicstore.entity.User;

public interface CartService {
    Cart getOrCreateCart(User user);
    Cart addItem(User user, Long productId, int quantity);
    Cart updateQuantity(User user, Long productId, int quantity);
    Cart removeItem(User user, Long productId);
    Cart applyOfferCode(User user, String code);
    Cart removeOfferCode(User user);
    Cart saveForLater(User user, Long productId);
    Cart moveToCartFromSaved(User user, Long productId);
    void clearCart(User user);
}
