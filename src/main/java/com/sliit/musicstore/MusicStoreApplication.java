package com.sliit.musicstore;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Web-based Music Store
 * SLIIT - SE2030 Software Engineering - Group Project
 *
 * Modules (one per group member):
 *  1. Product Management
 *  2. Order Management (Cart & Wishlist)
 *  3. Payment Management
 *  4. Instrument Rental Management
 *  5. Offer & Discount Management
 *  6. Review, Rating & Notification Management
 */
@SpringBootApplication
@EnableScheduling // used by OfferService to auto-expire offers and RentalService to flag overdue rentals
public class MusicStoreApplication {
    public static void main(String[] args) {
        SpringApplication.run(MusicStoreApplication.class, args);
    }
}
