package com.sliit.musicstore.service.impl;

import com.sliit.musicstore.entity.*;
import com.sliit.musicstore.repository.ProductRepository;
import com.sliit.musicstore.repository.WishlistItemRepository;
import com.sliit.musicstore.repository.WishlistRepository;
import com.sliit.musicstore.service.CartService;
import com.sliit.musicstore.service.WishlistService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * Member 2 - Order Management (Wishlist part).
 */
@Service
@Transactional
public class WishlistServiceImpl implements WishlistService {

    private final WishlistRepository wishlistRepository;
    private final WishlistItemRepository wishlistItemRepository;
    private final ProductRepository productRepository;
    private final CartService cartService;

    public WishlistServiceImpl(WishlistRepository wishlistRepository, WishlistItemRepository wishlistItemRepository,
                                ProductRepository productRepository, CartService cartService) {
        this.wishlistRepository = wishlistRepository;
        this.wishlistItemRepository = wishlistItemRepository;
        this.productRepository = productRepository;
        this.cartService = cartService;
    }

    @Override
    public Wishlist getOrCreateWishlist(User user) {
        return wishlistRepository.findByUserIdWithItems(user.getId()).orElseGet(() -> {
            Wishlist wishlist = new Wishlist();
            wishlist.setUser(user);
            return wishlistRepository.save(wishlist);
        });
    }

    @Override
    public Wishlist addItem(User user, Long productId) {
        Wishlist wishlist = getOrCreateWishlist(user);
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found: " + productId));

        boolean alreadyThere = wishlist.getItems().stream()
                .anyMatch(item -> item.getProduct() != null && item.getProduct().getId().equals(productId));
        if (!alreadyThere) {
            WishlistItem item = new WishlistItem();
            item.setWishlist(wishlist);
            item.setProduct(product);
            wishlist.getItems().add(item);
            wishlistRepository.save(wishlist);
        }
        return wishlistRepository.findByUserIdWithItems(user.getId()).orElse(wishlist);
    }

    @Override
    public Wishlist removeItem(User user, Long productId) {
        Wishlist wishlist = getOrCreateWishlist(user);
        boolean removed = wishlist.getItems().removeIf(item -> item.getProduct() != null && item.getProduct().getId().equals(productId));
        if (removed) {
            wishlistRepository.save(wishlist);
        }
        return wishlistRepository.findByUserIdWithItems(user.getId()).orElse(wishlist);
    }

    @Override
    public Wishlist moveToCart(User user, Long productId) {
        cartService.addItem(user, productId, 1);
        return removeItem(user, productId);
    }

    @Override
    public Wishlist moveAllToCart(User user) {
        Wishlist wishlist = getOrCreateWishlist(user);
        List<WishlistItem> itemsCopy = new ArrayList<>(wishlist.getItems());
        for (WishlistItem item : itemsCopy) {
            if (item.getProduct() != null && item.getProduct().isActive() && item.getProduct().isInStock() 
                    && (item.getProduct().getStockQuantity() == null || item.getProduct().getStockQuantity() > 0)) {
                cartService.addItem(user, item.getProduct().getId(), 1);
                wishlist.getItems().remove(item);
            }
        }
        wishlistRepository.save(wishlist);
        return wishlistRepository.findByUserIdWithItems(user.getId()).orElse(wishlist);
    }

    @Override
    public boolean contains(User user, Long productId) {
        Wishlist wishlist = getOrCreateWishlist(user);
        return wishlist.getItems().stream().anyMatch(i -> i.getProduct() != null && i.getProduct().getId().equals(productId));
    }

    @Override
    public boolean toggleItem(User user, Long productId) {
        if (contains(user, productId)) {
            removeItem(user, productId);
            return false; // removed
        } else {
            addItem(user, productId);
            return true; // added
        }
    }

    @Override
    public Wishlist updateItemCategory(User user, Long productId, String category) {
        Wishlist wishlist = getOrCreateWishlist(user);
        String catName = (category != null && !category.trim().isEmpty()) ? category.trim() : "Favorites";
        wishlist.getItems().stream()
                .filter(i -> i.getProduct() != null && i.getProduct().getId().equals(productId))
                .findFirst()
                .ifPresent(item -> {
                    item.setListCategory(catName);
                    wishlistItemRepository.save(item);
                });
        return wishlistRepository.findByUserIdWithItems(user.getId()).orElse(wishlist);
    }
}
