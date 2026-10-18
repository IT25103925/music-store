package com.sliit.musicstore.controller;

import com.sliit.musicstore.service.UserService;
import com.sliit.musicstore.service.WishlistService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

/** Member 2 - Order Management module (Wishlist part). */
@Controller
@RequestMapping("/wishlist")
public class WishlistController {

    private final WishlistService wishlistService;
    private final UserService userService;

    public WishlistController(WishlistService wishlistService, UserService userService) {
        this.wishlistService = wishlistService;
        this.userService = userService;
    }

    @GetMapping
    public String view(Model model, Principal principal) {
        model.addAttribute("wishlist", wishlistService.getOrCreateWishlist(userService.getCurrentUser(principal)));
        return "order/wishlist";
    }

    @PostMapping("/add")
    public String add(@RequestParam Long productId, Principal principal, @RequestHeader(value = "X-Requested-With", required = false) String requestedWith) {
        wishlistService.addItem(userService.getCurrentUser(principal), productId);
        return "redirect:/wishlist";
    }

    @PostMapping("/toggle")
    @ResponseBody
    public java.util.Map<String, Object> toggle(@RequestParam Long productId, Principal principal) {
        com.sliit.musicstore.entity.User user = userService.getCurrentUser(principal);
        java.util.Map<String, Object> response = new java.util.HashMap<>();
        if (user == null) {
            response.put("success", false);
            response.put("redirect", "/login");
            return response;
        }
        boolean added = wishlistService.toggleItem(user, productId);
        com.sliit.musicstore.entity.Wishlist wishlist = wishlistService.getOrCreateWishlist(user);
        int count = (wishlist != null && wishlist.getItems() != null) ? wishlist.getItems().size() : 0;
        response.put("success", true);
        response.put("inWishlist", added);
        response.put("count", count);
        return response;
    }

    @PostMapping("/update-category")
    public String updateCategory(@RequestParam Long productId, @RequestParam String category, Principal principal) {
        com.sliit.musicstore.entity.User user = userService.getCurrentUser(principal);
        if (user != null) {
            wishlistService.updateItemCategory(user, productId, category);
        }
        return "redirect:/wishlist";
    }

    @PostMapping("/remove")
    public String remove(@RequestParam Long productId, Principal principal) {
        wishlistService.removeItem(userService.getCurrentUser(principal), productId);
        return "redirect:/wishlist";
    }

    @PostMapping("/move-to-cart")
    public String moveToCart(@RequestParam Long productId, Principal principal) {
        wishlistService.moveToCart(userService.getCurrentUser(principal), productId);
        return "redirect:/cart";
    }

    @PostMapping("/move-all-to-cart")
    public String moveAllToCart(Principal principal) {
        wishlistService.moveAllToCart(userService.getCurrentUser(principal));
        return "redirect:/cart";
    }
}
