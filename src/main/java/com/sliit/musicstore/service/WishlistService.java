package com.sliit.musicstore.service;

import com.sliit.musicstore.entity.User;
import com.sliit.musicstore.entity.Wishlist;

public interface WishlistService {
    Wishlist getOrCreateWishlist(User user);
    Wishlist addItem(User user, Long productId);
    Wishlist removeItem(User user, Long productId);
    Wishlist moveToCart(User user, Long productId);
    Wishlist moveAllToCart(User user);
    boolean contains(User user, Long productId);
    boolean toggleItem(User user, Long productId);
    Wishlist updateItemCategory(User user, Long productId, String category);
}
