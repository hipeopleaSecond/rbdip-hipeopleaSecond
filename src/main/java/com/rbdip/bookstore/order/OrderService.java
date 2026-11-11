package com.rbdip.bookstore.order;

import com.rbdip.bookstore.customer.Customer;
import com.rbdip.bookstore.customer.CustomerRepository;
import com.rbdip.bookstore.product.Product;
import com.rbdip.bookstore.product.ProductRepository;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderService {

    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final CustomerRepository customerRepository;
    private final OrderValidator validator;
    private final OrderNotifier notifier;
    private final PricingCalculator pricingCalculator = new PricingCalculator();

    public OrderService(
            ProductRepository productRepository,
            OrderRepository orderRepository,
            OrderItemRepository orderItemRepository,
            CustomerRepository customerRepository,
            OrderValidator validator, OrderNotifier notifier) {
        this.customerRepository = customerRepository;
        this.validator = validator;
        this.notifier = notifier;
        this.productRepository = productRepository;
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
    }

    @Transactional
    public Order createOrder(CreateOrderRequest request) {
        validator.validate(request);

        List<PreparedItem> prepared = prepareItems(request.items());
        List<PricingCalculator.LineItem> lineItems = prepared.stream()
                .map(item -> new PricingCalculator.LineItem(item.product().getPrice(), item.quantity()))
                .toList();

        BigDecimal total = pricingCalculator.calculateOrderTotal(
                lineItems, request.customerType() == null ? "regular" : request.customerType(), request.couponCode());

        Order order = new Order(
                customerRepository.save(new Customer(
                        request.customerFullName(), request.customerAddress(), request.customerPhone())), "new");
        order = orderRepository.save(order);

        for (PreparedItem item : prepared) {
            orderItemRepository.save(new OrderItem(order.getId(), item.product(), item.quantity()));
        }

        notifier.send(request.customerFullName(), order.getId(), total);

        return order;
    }

    private List<PreparedItem> prepareItems(List<CreateOrderRequest.Item> items) {
        List<PreparedItem> prepared = new ArrayList<>();
        for (CreateOrderRequest.Item raw : items) {
            Product product = productRepository.findById(raw.productId())
                    .orElseThrow(() -> new IllegalArgumentException("product " + raw.productId() + " not found"));
            int quantity = raw.quantity() == null ? 1 : raw.quantity();
            if (quantity <= 0) {
                throw new IllegalArgumentException("quantity must be positive");
            }
            prepared.add(new PreparedItem(product, quantity));
        }
        return prepared;
    }

    private record PreparedItem(Product product, int quantity) {
    }
}
