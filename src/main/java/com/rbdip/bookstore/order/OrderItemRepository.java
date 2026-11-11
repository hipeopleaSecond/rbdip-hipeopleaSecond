package com.rbdip.bookstore.order;

import org.springframework.data.jpa.repository.Query;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    List<OrderItem> findByOrderId(Long orderId);
    @Query("select count(i) from OrderItem i, Order o "
            + "where i.orderId = o.id and i.product.id = :productId "
            + "and trim(concat(o.customer.firstName, ' ', o.customer.lastName)) = :customerName")
    long countPurchases(Long productId, String customerName);
}
