package com.sliit.musicstore.controller;

import com.sliit.musicstore.entity.User;
import com.sliit.musicstore.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/login")
    public String loginPage(HttpServletRequest request) {
        request.getSession(true);
        return "auth/login";
    }

    @GetMapping("/access-denied")
    public String accessDenied(Model model, java.security.Principal principal) {
        if (principal != null) {
            model.addAttribute("currentEmail", principal.getName());
        }
        return "error/access-denied";
    }

    @GetMapping("/register")
    public String registerPage(Model model, HttpServletRequest request) {
        request.getSession(true);
        model.addAttribute("user", new User());
        return "auth/register";
    }

    @PostMapping("/register")
    public String register(@RequestParam(required = false) String fullName, 
                            @RequestParam(required = false) String email,
                            @RequestParam(required = false) String password, 
                            @RequestParam(required = false) String phone,
                            @RequestParam(required = false) String address, Model model) {
        try {
            userService.register(fullName, email, password, phone, address);
            return "redirect:/login?registered=true";
        } catch (IllegalArgumentException ex) {
            model.addAttribute("error", ex.getMessage());
            model.addAttribute("fullName", fullName);
            model.addAttribute("email", email);
            model.addAttribute("phone", phone);
            model.addAttribute("address", address);
            return "auth/register";
        }
    }
}
