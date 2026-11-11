package com.rbdip.bookstore.order;

import com.rbdip.bookstore.api.PurchaseHistory;
import org.springframework.stereotype.Service;

@Service
public class OrderPurchaseHistory implements PurchaseHistory {
    private final OrderItemRepository items;

    public OrderPurchaseHistory(OrderItemRepository items) {
        this.items = items;
    }

    @Override
    public boolean hasPurchased(Long productId, String customerName) {
        return items.countPurchases(productId, customerName) > 0;
    }
}
