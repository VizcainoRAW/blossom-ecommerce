package com.blossom.ecommerce.infrastructure.web;

import com.blossom.ecommerce.support.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CategoryControllerIntegrationTest extends IntegrationTest {

    @Test
    void anyoneCanListAndGetCategories() throws Exception {
        mvc.perform(get("/api/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(5)))
                .andExpect(jsonPath("$[0].name").value("Accessories"));
        mvc.perform(get("/api/categories/{id}", SHIRTS))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Shirts"));
        mvc.perform(get("/api/categories/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void onlyEmployeesCanWrite() throws Exception {
        String customer = registerCustomer("ana@mail.com");

        mvc.perform(post("/api/categories").contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Hats\"}"))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/categories").header("Authorization", bearer(customer))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Hats\"}"))
                .andExpect(status().isForbidden());
        mvc.perform(delete("/api/categories/{id}", SHIRTS).header("Authorization", bearer(customer)))
                .andExpect(status().isForbidden());
    }

    @Test
    void employeeCreatesRenamesAndDeletes() throws Exception {
        String admin = bearer(adminToken());

        mvc.perform(post("/api/categories").header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"  Hats \"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Hats"));
        long id = jdbc.queryForObject("SELECT id FROM categories WHERE name = 'Hats'", Long.class);

        mvc.perform(put("/api/categories/{id}", id).header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Caps\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Caps"));

        mvc.perform(delete("/api/categories/{id}", id).header("Authorization", admin))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/categories/{id}", id)).andExpect(status().isNotFound());
    }

    @Test
    void duplicateNamesConflictIgnoringCase() throws Exception {
        String admin = bearer(adminToken());

        mvc.perform(post("/api/categories").header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"shirts\"}"))
                .andExpect(status().isConflict());
        mvc.perform(put("/api/categories/{id}", SHOES).header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"SHIRTS\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void blankNameIsRejected() throws Exception {
        mvc.perform(post("/api/categories").header("Authorization", bearer(adminToken()))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\" \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.name").exists());
    }

    @Test
    void categoryInUseCannotBeDeleted() throws Exception {
        createProduct("""
                {"categoryId":%d,"name":"Tee","variants":[{"color":"Black","size":"M","sku":"T-1","price":10,"stock":1}]}"""
                .formatted(SHIRTS));

        mvc.perform(delete("/api/categories/{id}", SHIRTS).header("Authorization", bearer(adminToken())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Category 1 still has products"));
    }
}
