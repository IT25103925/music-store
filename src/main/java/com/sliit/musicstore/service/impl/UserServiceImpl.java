package com.sliit.musicstore.service.impl;

import com.sliit.musicstore.entity.User;
import com.sliit.musicstore.repository.UserRepository;
import com.sliit.musicstore.service.UserService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.Principal;
import java.util.List;
import java.util.Optional;

@Service
@Transactional
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserServiceImpl(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    private static final String EMAIL_REGEX = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";
    private static final String PHONE_REGEX = "^\\+?[0-9\\s\\-]{9,15}$";

    @Override
    public User register(String fullName, String email, String rawPassword, String phone, String address) {
        String cleanFullName = fullName != null ? fullName.trim() : "";
        String cleanEmail = email != null ? email.trim().toLowerCase() : "";
        String cleanPhone = phone != null ? phone.trim() : "";
        String cleanAddress = address != null ? address.trim() : "";

        if (cleanFullName.isEmpty() || cleanEmail.isEmpty() || rawPassword == null || rawPassword.trim().isEmpty() || cleanPhone.isEmpty() || cleanAddress.isEmpty()) {
            throw new IllegalArgumentException("All fields (Full Name, Email, Password, Phone, Address) are required.");
        }

        if (!cleanEmail.matches(EMAIL_REGEX)) {
            throw new IllegalArgumentException("Please enter a valid email address (e.g., user@example.com).");
        }

        if (!cleanPhone.matches(PHONE_REGEX)) {
            throw new IllegalArgumentException("Please enter a valid phone number (e.g., 0771234567).");
        }

        if (rawPassword.length() < 6) {
            throw new IllegalArgumentException("Password must be at least 6 characters long.");
        }

        if (userRepository.existsByEmail(cleanEmail)) {
            throw new IllegalArgumentException("An account with this email already exists.");
        }

        User user = new User();
        user.setFullName(cleanFullName);
        user.setEmail(cleanEmail);
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setPhone(cleanPhone);
        user.setAddress(cleanAddress);
        user.setRole(User.Role.CUSTOMER);
        return userRepository.save(user);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    @Override
    public User getCurrentUser(Principal principal) {
        if (principal == null) return null;
        return userRepository.findByEmail(principal.getName()).orElse(null);
    }

    @Override
    public List<User> findAll() {
        return userRepository.findAll();
    }
}
