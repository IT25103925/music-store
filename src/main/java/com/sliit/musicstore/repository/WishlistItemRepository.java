package com.sliit.musicstore.repository;

import com.sliit.musicstore.entity.Product;
import com.sliit.musicstore.entity.Wishlist;
import com.sliit.musicstore.entity.WishlistItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface WishlistItemRepository extends JpaRepository<WishlistItem, Long> {
    Optional<WishlistItem> findByWishlistAndProduct(Wishlist wishlist, Product product);
    List<WishlistItem> findByProductId(Long productId);

    long countByProductId(Long productId);

    @Modifying
    @Transactional
    @Query("DELETE FROM WishlistItem wi WHERE wi.product.id = :productId")
    void deleteByProductId(@Param("productId") Long productId);
}
