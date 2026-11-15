package com.blossom.ecommerce.infrastructure.web;

import com.blossom.ecommerce.support.IntegrationTest;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class OrderControllerIntegrationTest extends IntegrationTest {

    String admin;
    String customer;
    long medium;   // TEE-B-M, 19.99, stock 10
    long large;    // TEE-B-L, 21.50, stock 2

    @BeforeEach
    void setUp() throws Exception {
        admin = bearer(adminToken());
        customer = bearer(registerCustomer("ana@mail.com"));
        createProduct("""
                {"categoryId":1,"name":"Tee","variants":[
                  {"color":"Black","size":"M","sku":"TEE-B-M","price":19.99,"stock":10},
                  {"color":"Black","size":"L","sku":"TEE-B-L","price":21.50,"stock":2}]}""");
        medium = jdbc.queryForObject("SELECT id FROM product_variants WHERE sku = 'TEE-B-M'", Long.class);
        large = jdbc.queryForObject("SELECT id FROM product_variants WHERE sku = 'TEE-B-L'", Long.class);
    }

    int stock(long variantId) {
        return jdbc.queryForObject("SELECT stock FROM product_variants WHERE id = ?", Integer.class, variantId);
    }

    String orderJson(long variantId, int quantity) {
        return """
                {"shippingAddress":"Calle 1 #2-3","items":[{"variantId":%d,"quantity":%d}]}""".formatted(variantId, quantity);
    }

    long placeOrder(String token, String json) throws Exception {
        String body = mvc.perform(post("/api/orders").header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$.id")).longValue();
    }

    @Test
    void customerPlacesOrderAtCurrentPricesAndStockIsTaken() throws Exception {
        mvc.perform(post("/api/orders").header("Authorization", customer).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"shippingAddress":"Calle 1 #2-3","items":[
                                  {"variantId":%d,"quantity":3},{"variantId":%d,"quantity":1}]}""".formatted(medium, large)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.total").value(81.47))
                .andExpect(jsonPath("$.shippingAddress").value("Calle 1 #2-3"))
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.items[0].productName").value("Tee"))
                .andExpect(jsonPath("$.items[0].sku").value("TEE-B-M"))
                .andExpect(jsonPath("$.items[0].unitPrice").value(19.99))
                .andExpect(jsonPath("$.items[0].subtotal").value(59.97));

        assertThat(stock(medium)).isEqualTo(7);
        assertThat(stock(large)).isEqualTo(1);
    }

    @Test
    void onlyCustomersCanPlaceOrders() throws Exception {
        mvc.perform(post("/api/orders").contentType(MediaType.APPLICATION_JSON).content(orderJson(medium, 1)))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/orders").header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON).content(orderJson(medium, 1)))
                .andExpect(status().isForbidden());
    }

    @Test
    void notEnoughStockRollsBackTheWholeOrder() throws Exception {
        mvc.perform(post("/api/orders").header("Authorization", customer).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"shippingAddress":"x","items":[
                                  {"variantId":%d,"quantity":1},{"variantId":%d,"quantity":3}]}""".formatted(medium, large)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Not enough stock for TEE-B-L"));

        assertThat(stock(medium)).isEqualTo(10);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM orders", Integer.class)).isZero();
    }

    @Test
    void invalidOrdersAreRejected() throws Exception {
        mvc.perform(post("/api/orders").header("Authorization", customer)
                        .contentType(MediaType.APPLICATION_JSON).content(orderJson(9999, 1)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Variant 9999 is not available"));
        mvc.perform(post("/api/orders").header("Authorization", customer).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"shippingAddress":"x","items":[
                                  {"variantId":%d,"quantity":1},{"variantId":%d,"quantity":1}]}""".formatted(medium, medium)))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/orders").header("Authorization", customer).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"items\":[{\"variantId\":1,\"quantity\":0}]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.shippingAddress").exists())
                .andExpect(jsonPath("$.errors['items[0].quantity']").exists());
    }

    @Test
    void hiddenProductsCannotBeOrdered() throws Exception {
        long productId = jdbc.queryForObject("SELECT product_id FROM product_variants WHERE id = ?", Long.class, medium);
        mvc.perform(delete("/api/products/{id}", productId).header("Authorization", admin))
                .andExpect(status().isNoContent());

        mvc.perform(post("/api/orders").header("Authorization", customer)
                        .contentType(MediaType.APPLICATION_JSON).content(orderJson(medium, 1)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void customersOnlySeeTheirOwnOrders() throws Exception {
        long id = placeOrder(customer, orderJson(medium, 1));
        String other = bearer(registerCustomer("other@mail.com"));
        String customerId = jdbc.queryForObject("SELECT customer_id::text FROM orders WHERE id = ?", String.class, id);

        mvc.perform(get("/api/orders/{id}", id).header("Authorization", customer)).andExpect(status().isOk());
        mvc.perform(get("/api/orders/{id}", id).header("Authorization", other)).andExpect(status().isNotFound());
        mvc.perform(get("/api/orders").header("Authorization", other))
                .andExpect(jsonPath("$.totalItems").value(0));
        // A customer can't widen their view with the customerId filter.
        mvc.perform(get("/api/orders").param("customerId", customerId).header("Authorization", other))
                .andExpect(jsonPath("$.totalItems").value(0));
        mvc.perform(get("/api/orders")).andExpect(status().isUnauthorized());
    }

    @Test
    void employeesSeeAllOrdersAndCanFilter() throws Exception {
        long first = placeOrder(customer, orderJson(medium, 1));
        placeOrder(bearer(registerCustomer("other@mail.com")), orderJson(medium, 1));
        String customerId = jdbc.queryForObject("SELECT customer_id::text FROM orders WHERE id = ?", String.class, first);

        mvc.perform(get("/api/orders").header("Authorization", admin))
                .andExpect(jsonPath("$.totalItems").value(2));
        mvc.perform(get("/api/orders").param("customerId", customerId).header("Authorization", admin))
                .andExpect(jsonPath("$.totalItems").value(1))
                .andExpect(jsonPath("$.items[0].id").value(first));
        mvc.perform(get("/api/orders").param("status", "SHIPPED").header("Authorization", admin))
                .andExpect(jsonPath("$.totalItems").value(0));
        mvc.perform(get("/api/orders/{id}", first).header("Authorization", admin)).andExpect(status().isOk());
    }

    @Test
    void employeeMovesOrderThroughItsLifecycle() throws Exception {
        long id = placeOrder(customer, orderJson(medium, 1));

        mvc.perform(patch("/api/orders/{id}/status", id).header("Authorization", customer)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"PAID\"}"))
                .andExpect(status().isForbidden());
        mvc.perform(patch("/api/orders/{id}/status", id).header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"SHIPPED\"}"))
                .andExpect(status().isConflict());
        mvc.perform(patch("/api/orders/{id}/status", id).header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"PAID\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("PAID"));
        mvc.perform(patch("/api/orders/{id}/status", id).header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"SHIPPED\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("SHIPPED"));
        mvc.perform(patch("/api/orders/{id}/status", id).header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"LOST\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(patch("/api/orders/9999/status").header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"PAID\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void cancellingReturnsStock() throws Exception {
        long pending = placeOrder(customer, orderJson(medium, 3));
        long paid = placeOrder(customer, orderJson(large, 2));
        mvc.perform(patch("/api/orders/{id}/status", paid).header("Authorization", admin)
                .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"PAID\"}")).andExpect(status().isOk());

        mvc.perform(post("/api/orders/{id}/cancel", pending).header("Authorization", customer))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("CANCELLED"));
        mvc.perform(post("/api/orders/{id}/cancel", paid).header("Authorization", customer))
                .andExpect(status().isConflict());
        mvc.perform(patch("/api/orders/{id}/status", paid).header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"CANCELLED\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("CANCELLED"));
        mvc.perform(post("/api/orders/{id}/cancel", paid).header("Authorization", admin))
                .andExpect(status().isConflict());

        assertThat(stock(medium)).isEqualTo(10);
        assertThat(stock(large)).isEqualTo(2);
    }

    @Test
    void concurrentCheckoutsNeverOversell() throws Exception {
        // Only 2 of TEE-B-L exist; 8 customers try to buy one each at the same time.
        List<String> tokens = new ArrayList<>();
        for (int i = 0; i < 8; i++) tokens.add(bearer(registerCustomer("buyer" + i + "@mail.com")));

        List<Callable<Integer>> attempts = tokens.stream().<Callable<Integer>>map(token -> () ->
                mvc.perform(post("/api/orders").header("Authorization", token)
                                .contentType(MediaType.APPLICATION_JSON).content(orderJson(large, 1)))
                        .andReturn().getResponse().getStatus()).toList();

        List<Integer> statuses = new ArrayList<>();
        try (ExecutorService pool = Executors.newFixedThreadPool(8)) {
            for (Future<Integer> f : pool.invokeAll(attempts)) statuses.add(f.get());
        }

        assertThat(statuses).filteredOn(s -> s == 201).hasSize(2);
        assertThat(statuses).filteredOn(s -> s == 409).hasSize(6);
        assertThat(stock(large)).isZero();
    }
}
