package com.sliit.musicstore.controller;

import com.sliit.musicstore.entity.Cart;
import com.sliit.musicstore.entity.Offer;
import com.sliit.musicstore.entity.User;
import com.sliit.musicstore.service.CartService;
import com.sliit.musicstore.service.OfferService;
import com.sliit.musicstore.service.UserService;
import com.sliit.musicstore.service.WishlistService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.security.Principal;
import java.util.Optional;

/** Member 2 - Order Management module (Cart part). */
@Controller
@RequestMapping("/cart")
public class CartController {

    private final CartService cartService;
    private final UserService userService;
    private final OfferService offerService;
    private final WishlistService wishlistService;

    public CartController(CartService cartService, UserService userService, OfferService offerService, WishlistService wishlistService) {
        this.cartService = cartService;
        this.userService = userService;
        this.offerService = offerService;
        this.wishlistService = wishlistService;
    }

    @GetMapping
    public String view(Model model, Principal principal) {
        User user = userService.getCurrentUser(principal);
        Cart cart = cartService.getOrCreateCart(user);

        java.util.List<com.sliit.musicstore.entity.CartItem> activeItems = cart.getItems().stream()
                .filter(item -> !item.isSavedForLater())
                .collect(java.util.stream.Collectors.toList());

        java.util.List<com.sliit.musicstore.entity.CartItem> savedItems = cart.getItems().stream()
                .filter(com.sliit.musicstore.entity.CartItem::isSavedForLater)
                .collect(java.util.stream.Collectors.toList());

        BigDecimal subtotal = activeItems.stream()
                .map(item -> item.getSubtotal())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal discount = BigDecimal.ZERO;
        if (cart.getAppliedOfferCode() != null) {
            Optional<Offer> offer = offerService.findValidByCode(cart.getAppliedOfferCode());
            if (offer.isPresent()) {
                discount = offerService.calculateDiscount(offer.get(), subtotal, activeItems);
                if (offer.get().getMinOrderAmount() != null && subtotal.compareTo(offer.get().getMinOrderAmount()) < 0) {
                    model.addAttribute("offerNotice", "Coupon code '" + cart.getAppliedOfferCode() + "' requires a minimum purchase of LKR " + String.format("%,.0f", offer.get().getMinOrderAmount()) + " to unlock discount.");
                }
            }
        }

        model.addAttribute("cart", cart);
        model.addAttribute("activeItems", activeItems);
        model.addAttribute("savedItems", savedItems);
        model.addAttribute("subtotal", subtotal);
        model.addAttribute("discount", discount);
        model.addAttribute("total", subtotal.subtract(discount));
        model.addAttribute("availableOffers", offerService.findAllActive());
        return "order/cart";
    }

    @PostMapping("/add")
    public String add(@RequestParam Long productId,
                      @RequestParam(defaultValue = "1") int quantity,
                      @RequestParam(required = false, defaultValue = "false") boolean buyNow,
                      @RequestParam(required = false) String offerCode,
                      Principal principal,
                      org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes) {
        User user = userService.getCurrentUser(principal);
        cartService.addItem(user, productId, quantity);
        if (offerCode != null && !offerCode.isBlank()) {
            try {
                cartService.applyOfferCode(user, offerCode.trim());
                redirectAttributes.addFlashAttribute("success", "Offer code '" + offerCode.trim().toUpperCase() + "' applied successfully!");
            } catch (Exception ex) {
                redirectAttributes.addFlashAttribute("offerError", ex.getMessage());
            }
        }
        if (buyNow) {
            return "redirect:/orders/checkout";
        }
        return "redirect:/cart";
    }

    @PostMapping("/update")
    public String update(@RequestParam Long productId, @RequestParam int quantity, Principal principal) {
        cartService.updateQuantity(userService.getCurrentUser(principal), productId, quantity);
        return "redirect:/cart";
    }

    @PostMapping("/remove")
    public String remove(@RequestParam Long productId, Principal principal) {
        cartService.removeItem(userService.getCurrentUser(principal), productId);
        return "redirect:/cart";
    }

    @PostMapping("/move-to-wishlist")
    public String moveToWishlist(@RequestParam Long productId, Principal principal) {
        User user = userService.getCurrentUser(principal);
        if (user != null) {
            wishlistService.addItem(user, productId);
            cartService.removeItem(user, productId);
        }
        return "redirect:/cart";
    }

    @PostMapping("/save-for-later")
    public String saveForLater(@RequestParam Long productId, Principal principal) {
        User user = userService.getCurrentUser(principal);
        if (user != null) {
            cartService.saveForLater(user, productId);
        }
        return "redirect:/cart";
    }

    @PostMapping("/move-to-cart-from-saved")
    public String moveToCartFromSaved(@RequestParam Long productId, Principal principal) {
        User user = userService.getCurrentUser(principal);
        if (user != null) {
            cartService.moveToCartFromSaved(user, productId);
        }
        return "redirect:/cart";
    }

    @PostMapping("/clear")
    public String clearCart(Principal principal, org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes) {
        User user = userService.getCurrentUser(principal);
        if (user != null) {
            cartService.clearCart(user);
            redirectAttributes.addFlashAttribute("success", "Your shopping cart has been cleared.");
        }
        return "redirect:/cart";
    }

    @PostMapping("/apply-offer")
    public String applyOffer(@RequestParam String code,
                             @RequestParam(required = false) Long productId,
                             Principal principal,
                             org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes) {
        User user = userService.getCurrentUser(principal);
        if (user == null) {
            return "redirect:/login";
        }
        if (productId != null) {
            try {
                cartService.addItem(user, productId, 1);
            } catch (Exception ex) {
                redirectAttributes.addFlashAttribute("offerError", ex.getMessage());
            }
        }
        try {
            cartService.applyOfferCode(user, code);
            redirectAttributes.addFlashAttribute("success", "Offer code '" + code.toUpperCase() + "' applied successfully!");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("offerError", ex.getMessage());
        }
        return "redirect:/cart";
    }

    @PostMapping("/remove-offer")
    public String removeOffer(Principal principal) {
        cartService.removeOfferCode(userService.getCurrentUser(principal));
        return "redirect:/cart";
    }

    @GetMapping({"/add", "/update", "/remove", "/move-to-wishlist", "/save-for-later", "/move-to-cart-from-saved", "/clear", "/apply-offer", "/remove-offer"})
    public String fallbackGet() {
        return "redirect:/cart";
    }
}
