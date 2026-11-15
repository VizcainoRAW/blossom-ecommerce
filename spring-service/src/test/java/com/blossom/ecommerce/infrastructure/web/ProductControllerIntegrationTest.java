package com.blossom.ecommerce.infrastructure.web;

import com.blossom.ecommerce.support.IntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ProductControllerIntegrationTest extends IntegrationTest {

    static final String LINEN_SHIRT = """
            {"categoryId":1,"name":"Linen Shirt","description":"Light","imageUrls":["https://img/1.jpg"],
             "variants":[{"color":"White","size":"M","sku":"LS-W-M","price":49.90,"stock":10},
                         {"color":"White","size":"L","sku":"LS-W-L","price":59.90,"stock":5}]}""";
    static final String RUNNING_SHOE = """
            {"categoryId":4,"name":"Running Shoe",
             "variants":[{"color":"Black","size":"42","sku":"RS-B-42","price":120,"stock":3}]}""";

    String admin;

    @BeforeEach
    void setUp() throws Exception {
        admin = bearer(adminToken());
    }

    @Test
    void employeeCreatesAndAnyoneReads() throws Exception {
        long id = createProduct(LINEN_SHIRT);

        mvc.perform(get("/api/products/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Linen Shirt"))
                .andExpect(jsonPath("$.categoryId").value(1))
                .andExpect(jsonPath("$.imageUrls[0]").value("https://img/1.jpg"))
                .andExpect(jsonPath("$.variants", hasSize(2)))
                .andExpect(jsonPath("$.variants[0].sku").value("LS-W-M"))
                .andExpect(jsonPath("$.variants[0].price").value(49.90))
                .andExpect(jsonPath("$.variants[0].id").isNumber());
    }

    @Test
    void customersAndAnonymousCannotWrite() throws Exception {
        String customer = bearer(registerCustomer("ana@mail.com"));

        mvc.perform(post("/api/products").contentType(MediaType.APPLICATION_JSON).content(LINEN_SHIRT))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/products").header("Authorization", customer)
                        .contentType(MediaType.APPLICATION_JSON).content(LINEN_SHIRT))
                .andExpect(status().isForbidden());
    }

    @Test
    void invalidBodyListsFieldErrors() throws Exception {
        mvc.perform(post("/api/products").header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"name":"","variants":[{"color":"","size":"M","sku":"X","price":-1,"stock":1}]}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.categoryId").exists())
                .andExpect(jsonPath("$.errors.name").exists())
                .andExpect(jsonPath("$.errors['variants[0].color']").exists())
                .andExpect(jsonPath("$.errors['variants[0].price']").exists());
    }

    @Test
    void unknownCategoryAndDuplicateVariantsAreBadRequests() throws Exception {
        mvc.perform(post("/api/products").header("Authorization", admin).contentType(MediaType.APPLICATION_JSON)
                        .content(LINEN_SHIRT.replace("\"categoryId\":1", "\"categoryId\":999")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Category 999 does not exist"));
        mvc.perform(post("/api/products").header("Authorization", admin).contentType(MediaType.APPLICATION_JSON)
                        .content(LINEN_SHIRT.replace("LS-W-L", "LS-W-M")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Duplicate SKU LS-W-M"));
    }

    @Test
    void skuUsedByAnotherProductConflicts() throws Exception {
        createProduct(LINEN_SHIRT);

        mvc.perform(post("/api/products").header("Authorization", admin).contentType(MediaType.APPLICATION_JSON)
                        .content(RUNNING_SHOE.replace("RS-B-42", "LS-W-M")))
                .andExpect(status().isConflict());
    }

    @Test
    void searchFiltersByNameCategoryAndPrice() throws Exception {
        createProduct(LINEN_SHIRT);
        createProduct(RUNNING_SHOE);

        mvc.perform(get("/api/products").param("name", "LINEN"))
                .andExpect(jsonPath("$.totalItems").value(1))
                .andExpect(jsonPath("$.items[0].name").value("Linen Shirt"));
        mvc.perform(get("/api/products").param("categoryId", String.valueOf(SHOES)))
                .andExpect(jsonPath("$.items[*].name", containsInAnyOrder("Running Shoe")));
        // Matches if any variant is in range: only the L shirt (59.90) qualifies.
        mvc.perform(get("/api/products").param("minPrice", "55").param("maxPrice", "100"))
                .andExpect(jsonPath("$.items[*].name", containsInAnyOrder("Linen Shirt")));
        mvc.perform(get("/api/products").param("minPrice", "100"))
                .andExpect(jsonPath("$.items[*].name", containsInAnyOrder("Running Shoe")));
        mvc.perform(get("/api/products").param("maxPrice", "10"))
                .andExpect(jsonPath("$.totalItems").value(0));
        // LIKE wildcards in the search term are literal.
        mvc.perform(get("/api/products").param("name", "%"))
                .andExpect(jsonPath("$.totalItems").value(0));
    }

    @Test
    void searchPaginates() throws Exception {
        createProduct(LINEN_SHIRT);
        createProduct(RUNNING_SHOE);

        mvc.perform(get("/api/products").param("page", "1").param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.items[0].name").value("Running Shoe"))
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.size").value(1))
                .andExpect(jsonPath("$.totalItems").value(2));
    }

    @Test
    void updateMatchesVariantsBySku() throws Exception {
        long id = createProduct(LINEN_SHIRT);
        Number keptVariantId = jdbc.queryForObject("SELECT id FROM product_variants WHERE sku = 'LS-W-M'", Long.class);

        // Keeps M (new price), drops LS-W-L and adds a new SKU with the same color/size it had.
        mvc.perform(put("/api/products/{id}", id).header("Authorization", admin).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"categoryId":2,"name":"Linen Shirt v2",
                                 "variants":[{"color":"White","size":"M","sku":"LS-W-M","price":39.90,"stock":8},
                                             {"color":"White","size":"L","sku":"LS-W-L2","price":65,"stock":2}]}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Linen Shirt v2"))
                .andExpect(jsonPath("$.categoryId").value(2))
                .andExpect(jsonPath("$.variants[0].id").value(keptVariantId.intValue()))
                .andExpect(jsonPath("$.variants[0].price").value(39.90))
                .andExpect(jsonPath("$.variants[*].sku", containsInAnyOrder("LS-W-M", "LS-W-L2")));
    }

    @Test
    void deleteHidesProduct() throws Exception {
        long id = createProduct(LINEN_SHIRT);

        mvc.perform(delete("/api/products/{id}", id).header("Authorization", admin)).andExpect(status().isNoContent());

        mvc.perform(get("/api/products/{id}", id)).andExpect(status().isNotFound());
        mvc.perform(get("/api/products")).andExpect(jsonPath("$.totalItems").value(0));
        mvc.perform(put("/api/products/{id}", id).header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON).content(LINEN_SHIRT))
                .andExpect(status().isNotFound());
        mvc.perform(delete("/api/products/{id}", id).header("Authorization", admin)).andExpect(status().isNotFound());
    }
}
