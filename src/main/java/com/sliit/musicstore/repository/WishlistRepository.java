package com.sliit.musicstore.repository;

import com.sliit.musicstore.entity.Wishlist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface WishlistRepository extends JpaRepository<Wishlist, Long> {
    Optional<Wishlist> findByUserId(Long userId);

    /**
     * Loads the wishlist together with its items and each item's product in one
     * query, so the collection is already populated by the time the transaction
     * closes (needed because spring.jpa.open-in-view=false means lazy collections
     * can't be touched later, e.g. while Thymeleaf renders order/wishlist.html).
     */
    @Query("SELECT DISTINCT w FROM Wishlist w LEFT JOIN FETCH w.items i LEFT JOIN FETCH i.product WHERE w.user.id = :userId")
    Optional<Wishlist> findByUserIdWithItems(@Param("userId") Long userId);
}
