package com.rbdip.bookstore.api;

/** Public contract shared by the order and review modules. */
public interface PurchaseHistory {
    boolean hasPurchased(Long productId, String customerName);
}
