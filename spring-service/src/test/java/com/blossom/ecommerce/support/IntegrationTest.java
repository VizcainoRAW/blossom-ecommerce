package com.blossom.ecommerce.support;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.MountableFile;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Boots the full app against a real Postgres built from deployment/database, so tests hit the same
 * schema (arrays, deferred constraints, row locks) as production. One container is shared by all test classes.
 */
@SpringBootTest(properties = "security.jwt.secret=integration-test-secret-at-least-32-bytes")
@AutoConfigureMockMvc
public abstract class IntegrationTest {

    public static final String ADMIN_EMAIL = "admin@blossom.dev";
    public static final String ADMIN_PASSWORD = "admin123";
    /** Seeded by 02_dev_seed.sql. */
    public static final long SHIRTS = 1, SHOES = 4;

    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:18-alpine")
            .withCopyFileToContainer(MountableFile.forHostPath("../deployment/database/01_init.sql"),
                    "/docker-entrypoint-initdb.d/01_init.sql")
            .withCopyFileToContainer(MountableFile.forHostPath("../deployment/database/02_dev_seed.sql"),
                    "/docker-entrypoint-initdb.d/02_dev_seed.sql");

    static {
        POSTGRES.start();
    }

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired
    protected MockMvc mvc;

    @Autowired
    protected JdbcTemplate jdbc;

    /** Back to the seed state: admin employee and the five seeded categories. */
    @BeforeEach
    void resetDatabase() {
        jdbc.execute("TRUNCATE order_items, orders, product_variants, products RESTART IDENTITY CASCADE");
        jdbc.update("DELETE FROM customers");
        jdbc.update("DELETE FROM accounts WHERE account_type = 'CUSTOMER'");
        jdbc.update("DELETE FROM categories WHERE id > 5");
    }

    protected String adminToken() throws Exception {
        return login(ADMIN_EMAIL, ADMIN_PASSWORD);
    }

    protected String login(String identifier, String password) throws Exception {
        String body = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"identifier":"%s","password":"%s"}""".formatted(identifier, password)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.accessToken");
    }

    /** Registers a customer with password "secret123" and returns their token. */
    protected String registerCustomer(String email) throws Exception {
        String body = mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"firstName":"Ana","lastName":"Diaz","identifier":"%s","password":"secret123"}"""
                                .formatted(email)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.accessToken");
    }

    /** Creates a product as admin and returns its id. */
    protected long createProduct(String json) throws Exception {
        String body = mvc.perform(post("/api/products").header("Authorization", bearer(adminToken()))
                        .contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(body, "$.id")).longValue();
    }

    protected static String bearer(String token) {
        return "Bearer " + token;
    }
}
