package com.rbdip.bookstore;

import static org.assertj.core.api.Assertions.assertThat;

import com.rbdip.bookstore.order.CreateOrderRequest;
import com.rbdip.bookstore.reference.AbstractIntegrationTest;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class BookstoreIntegrationTest extends AbstractIntegrationTest {
    @org.springframework.beans.factory.annotation.Autowired
    private com.rbdip.bookstore.review.ReviewService reviewService;
    @org.springframework.beans.factory.annotation.Autowired
    private com.rbdip.bookstore.order.OrderRepository orderRepository;
    @Test
    void catalogOrdersAndReviewsKeepTheirPublicContract() {
        Map product = restTemplate.postForObject("/products",
                Map.of("name", "Book", "price", "12.50", "description", "Text"), Map.class);
        Long id = Long.valueOf(product.get("id").toString());
        assertThat(restTemplate.getForObject("/products", List.class)).hasSize(1);
        var request = new CreateOrderRequest("Single", "Address", "123", "vip", "SAVE10",
                List.of(new CreateOrderRequest.Item(id, null)));
        assertThat(restTemplate.postForEntity("/orders", request, Map.class).getStatusCode())
                .isEqualTo(HttpStatus.CREATED);
        assertThat(reviewService.isVerifiedPurchase(id, "Single")).isTrue();
        assertThat(reviewService.isVerifiedPurchase(id, "Other")).isFalse();
        var saved = orderRepository.findAllWithDetails().get(0);
        assertThat(saved.getCustomerAddress()).isEqualTo("Address");
        assertThat(saved.getCustomerPhone()).isEqualTo("123");
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getItems().get(0).getProductPrice()).isEqualByComparingTo("12.50");
        assertThat(saved.getItems().get(0).getOrderId()).isEqualTo(saved.getId());
        assertThat(saved.getItems().get(0).getId()).isNotNull();
        List<Map> orders = restTemplate.getForObject("/orders", List.class);
        assertThat(orders).hasSize(1);
        assertThat(orders.get(0)).containsEntry("customerFullName", "Single");
        assertThat((List<?>) orders.get(0).get("items")).hasSize(1);
        assertThat(restTemplate.postForEntity("/products/" + id + "/reviews",
                Map.of("rating", 5), Map.class).getStatusCode()).isEqualTo(HttpStatus.CREATED);
        List<Map> reviews = restTemplate.getForObject("/products/" + id + "/reviews", List.class);
        assertThat(reviews.get(0)).containsEntry("authorName", "anonymous").containsEntry("comment", "");
    }

    @Test
    void rejectsInvalidOrders() {
        for (var request : List.of(
                new CreateOrderRequest("", "Address", null, null, null, List.of()),
                new CreateOrderRequest("Name", "", null, null, null, List.of()),
                new CreateOrderRequest("Name", "Address", null, null, null, List.of()),
                new CreateOrderRequest("Name", "Address", null, null, null,
                        List.of(new CreateOrderRequest.Item(999L, 1))))) {
            assertThat(restTemplate.postForEntity("/orders", request, Map.class).getStatusCode())
                    .isEqualTo(HttpStatus.BAD_REQUEST);
        }
    }
}
