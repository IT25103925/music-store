package com.sliit.musicstore.service;

import com.sliit.musicstore.entity.User;

import java.util.List;
import java.util.Optional;

public interface UserService {
    User register(String fullName, String email, String rawPassword, String phone, String address);
    Optional<User> findByEmail(String email);
    User getCurrentUser(java.security.Principal principal);
    List<User> findAll();
}
