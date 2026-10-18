package com.sliit.musicstore.repository;

import com.sliit.musicstore.entity.Cart;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CartRepository extends JpaRepository<Cart, Long> {
    Optional<Cart> findByUserId(Long userId);

    /**
     * Loads the cart together with its items and each item's product in one query,
     * so the collection is already populated by the time the transaction closes
     * (needed because spring.jpa.open-in-view=false means lazy collections can't
     * be touched later, e.g. while Thymeleaf renders order/cart.html).
     */
    @Query("SELECT DISTINCT c FROM Cart c LEFT JOIN FETCH c.items i LEFT JOIN FETCH i.product WHERE c.user.id = :userId")
    Optional<Cart> findByUserIdWithItems(@Param("userId") Long userId);
}
