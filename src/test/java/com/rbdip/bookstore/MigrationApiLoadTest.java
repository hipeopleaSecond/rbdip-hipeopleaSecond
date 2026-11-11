package com.rbdip.bookstore;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

/** Exercises the new application while the final contract migration runs. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "spring.flyway.target=3")
class MigrationApiLoadTest {
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");
    static {
        POSTGRES.start();
    }

    @DynamicPropertySource
    static void database(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired
    private TestRestTemplate api;
    @Autowired
    private DataSource dataSource;

    @Test
    void contractKeepsApiReadsAndWritesAvailable() throws Exception {
        var running = new AtomicBoolean(true);
        var requests = new AtomicInteger();
        List<Throwable> errors = new CopyOnWriteArrayList<>();
        var pool = Executors.newFixedThreadPool(2);
        Map product = api.postForObject("/products", Map.of("name", "Book", "price", "10"), Map.class);
        Map<String, Object> order = Map.of("customerFullName", "Test Customer", "customerAddress", "Street",
                "items", List.of(Map.of("productId", product.get("id"), "quantity", 1)));
        for (int i = 0; i < 2; i++) {
            pool.submit(() -> {
                while (running.get()) {
                    try {
                        assertThat(api.postForEntity("/orders", order, Map.class).getStatusCode().value()).isEqualTo(201);
                        assertThat(api.getForEntity("/orders", List.class).getStatusCode().value()).isEqualTo(200);
                        requests.incrementAndGet();
                    } catch (Throwable error) {
                        errors.add(error);
                    }
                }
            });
        }
        try {
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
            while (requests.get() < 2 && errors.isEmpty() && System.nanoTime() < deadline) {
                Thread.sleep(10);
            }
            Flyway.configure().dataSource(dataSource).load().migrate();
            assertThat(api.postForEntity("/orders", order, Map.class).getStatusCode().value()).isEqualTo(201);
        } finally {
            running.set(false);
            pool.shutdown();
            assertThat(pool.awaitTermination(10, TimeUnit.SECONDS)).isTrue();
        }
        assertThat(requests.get()).isGreaterThanOrEqualTo(2);
        assertThat(errors).isEmpty();
    }
}
