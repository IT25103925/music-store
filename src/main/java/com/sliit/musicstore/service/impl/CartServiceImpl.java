package com.sliit.musicstore.service.impl;

import com.sliit.musicstore.entity.*;
import com.sliit.musicstore.repository.CartItemRepository;
import com.sliit.musicstore.repository.CartRepository;
import com.sliit.musicstore.repository.ProductRepository;
import com.sliit.musicstore.repository.WishlistRepository;
import com.sliit.musicstore.service.CartService;
import com.sliit.musicstore.service.OfferService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Member 2 - Order Management (Cart part).
 */
@Service
@Transactional
public class CartServiceImpl implements CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final OfferService offerService;
    private final WishlistRepository wishlistRepository;

    public CartServiceImpl(CartRepository cartRepository, CartItemRepository cartItemRepository,
                            ProductRepository productRepository, OfferService offerService,
                            WishlistRepository wishlistRepository) {
        this.cartRepository = cartRepository;
        this.cartItemRepository = cartItemRepository;
        this.productRepository = productRepository;
        this.offerService = offerService;
        this.wishlistRepository = wishlistRepository;
    }

    @Override
    public Cart getOrCreateCart(User user) {
        return cartRepository.findByUserIdWithItems(user.getId()).orElseGet(() -> {
            Cart cart = new Cart();
            cart.setUser(user);
            return cartRepository.save(cart);
        });
    }

    @Override
    public Cart addItem(User user, Long productId, int quantity) {
        Cart cart = getOrCreateCart(user);
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found: " + productId));

        int availableStock = product.getStockQuantity() == null ? 99 : product.getStockQuantity();
        if (availableStock <= 0) {
            throw new IllegalStateException("'" + product.getName() + "' is currently out of stock.");
        }

        CartItem item = cartItemRepository.findByCartAndProduct(cart, product).orElseGet(() -> {
            CartItem newItem = new CartItem();
            newItem.setCart(cart);
            newItem.setProduct(product);
            newItem.setQuantity(0);
            cart.getItems().add(newItem);
            return newItem;
        });
        item.setProductName(product.getName());
        item.setUnitPrice(product.getPrice());
        item.setSavedForLater(false);

        int targetQty = Math.min(item.getQuantity() + quantity, availableStock);
        if (targetQty <= 0) {
            throw new IllegalStateException("'" + product.getName() + "' is currently out of stock.");
        }
        item.setQuantity(targetQty);
        cartItemRepository.save(item);

        // Remove from wishlist if it exists in the user's wishlist
        wishlistRepository.findByUserIdWithItems(user.getId()).ifPresent(wishlist -> {
            boolean removed = wishlist.getItems().removeIf(wItem -> wItem.getProduct() != null && wItem.getProduct().getId().equals(productId));
            if (removed) {
                wishlistRepository.save(wishlist);
            }
        });

        return cartRepository.findById(cart.getId()).orElse(cart);
    }

    @Override
    public Cart updateQuantity(User user, Long productId, int quantity) {
        Cart cart = getOrCreateCart(user);
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found: " + productId));
        cartItemRepository.findByCartAndProduct(cart, product).ifPresent(item -> {
            if (quantity <= 0) {
                cart.getItems().remove(item);
                cartItemRepository.delete(item);
            } else {
                int maxStock = product.getStockQuantity() == null ? 99 : Math.max(0, product.getStockQuantity());
                if (maxStock <= 0) {
                    cart.getItems().remove(item);
                    cartItemRepository.delete(item);
                    throw new IllegalStateException("'" + product.getName() + "' is currently out of stock.");
                }
                item.setQuantity(Math.min(quantity, maxStock));
                cartItemRepository.save(item);
            }
        });
        return cartRepository.findById(cart.getId()).orElse(cart);
    }

    @Override
    public Cart removeItem(User user, Long productId) {
        return updateQuantity(user, productId, 0);
    }

    @Override
    public Cart applyOfferCode(User user, String code) {
        Cart cart = getOrCreateCart(user);
        Offer offer = offerService.findValidByCode(code)
                .orElseThrow(() -> new IllegalArgumentException("Invalid or expired offer code: " + code));

        java.util.List<CartItem> activeItems = cart.getItems().stream()
                .filter(item -> !item.isSavedForLater())
                .collect(java.util.stream.Collectors.toList());

        java.math.BigDecimal subtotal = activeItems.stream()
                .map(CartItem::getSubtotal)
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);

        if (offer.getCategory() != null) {
            boolean hasMatchingCategory = activeItems.stream()
                    .anyMatch(item -> item.getProduct() != null && item.getProduct().getCategory() != null &&
                            item.getProduct().getCategory().getId().equals(offer.getCategory().getId()));
            if (!hasMatchingCategory) {
                throw new IllegalArgumentException("Offer code '" + code.toUpperCase() + "' is only applicable to items in the '" + offer.getCategory().getName() + "' category.");
            }
        }

        if (offer.getMinOrderAmount() != null && subtotal.compareTo(offer.getMinOrderAmount()) < 0) {
            throw new IllegalArgumentException("Offer code '" + code.toUpperCase() + "' requires a minimum purchase of LKR " + String.format("%,.0f", offer.getMinOrderAmount()) + ". Add more items to your cart to qualify.");
        }

        cart.setAppliedOfferCode(code.toUpperCase());
        return cartRepository.save(cart);
    }

    @Override
    public Cart removeOfferCode(User user) {
        Cart cart = getOrCreateCart(user);
        cart.setAppliedOfferCode(null);
        return cartRepository.save(cart);
    }

    @Override
    public Cart saveForLater(User user, Long productId) {
        Cart cart = getOrCreateCart(user);
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found: " + productId));
        cartItemRepository.findByCartAndProduct(cart, product).ifPresent(item -> {
            item.setSavedForLater(true);
            cartItemRepository.save(item);
        });
        return cartRepository.findById(cart.getId()).orElse(cart);
    }

    @Override
    public Cart moveToCartFromSaved(User user, Long productId) {
        Cart cart = getOrCreateCart(user);
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found: " + productId));
        cartItemRepository.findByCartAndProduct(cart, product).ifPresent(item -> {
            item.setSavedForLater(false);
            cartItemRepository.save(item);
        });
        return cartRepository.findById(cart.getId()).orElse(cart);
    }

    @Override
    public void clearCart(User user) {
        Cart cart = getOrCreateCart(user);
        cart.getItems().clear();
        cart.setAppliedOfferCode(null);
        cartRepository.save(cart);
    }
}
