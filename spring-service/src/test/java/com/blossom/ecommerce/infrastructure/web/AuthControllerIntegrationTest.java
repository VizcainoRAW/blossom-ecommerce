package com.blossom.ecommerce.infrastructure.web;

import com.blossom.ecommerce.support.IntegrationTest;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthControllerIntegrationTest extends IntegrationTest {

    private static String claims(String token) {
        return new String(Base64.getUrlDecoder().decode(token.split("\\.")[1]), StandardCharsets.UTF_8);
    }

    @Test
    void registerReturnsJwtWithUserIdAndAccountType() throws Exception {
        String body = mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content("""
                        {"firstName":"Ana","lastName":"Diaz","identifier":"ana@mail.com","password":"secret123",
                         "documentType":"PASSPORT","documentNumber":"AB123"}"""))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(3600))
                .andReturn().getResponse().getContentAsString();

        String claims = claims(JsonPath.read(body, "$.accessToken"));
        String userId = JsonPath.read(claims, "$.user_id");
        assertThat(userId).isEqualTo(JsonPath.read(claims, "$.sub"));
        assertThat((String) JsonPath.read(claims, "$.account_type")).isEqualTo("CUSTOMER");
        assertThat(jdbc.queryForObject("SELECT identifier FROM accounts WHERE id = ?::uuid", String.class, userId))
                .isEqualTo("ana@mail.com");
    }

    @Test
    void registerRejectsDuplicateIdentifierInAnyCase() throws Exception {
        registerCustomer("ana@mail.com");

        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content("""
                        {"firstName":"Ana","lastName":"Diaz","identifier":"ANA@mail.com","password":"secret123"}"""))
                .andExpect(status().isConflict());
    }

    @Test
    void registerCannotReuseAnEmployeeEmail() throws Exception {
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content("""
                        {"firstName":"Ana","lastName":"Diaz","identifier":"%s","password":"secret123"}""".formatted(ADMIN_EMAIL)))
                .andExpect(status().isConflict());
    }

    @Test
    void registerReportsEachInvalidField() throws Exception {
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content("""
                        {"firstName":"","lastName":"Diaz","identifier":"ana@mail.com","password":"short"}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.firstName", notNullValue()))
                .andExpect(jsonPath("$.errors.password", notNullValue()));
    }

    @Test
    void registerMapsDomainRulesToBadRequest() throws Exception {
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content("""
                        {"firstName":"Ana","lastName":"Diaz","identifier":"ana@mail.com","password":"secret123",
                         "documentType":"CC"}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Document type and number go together"));
    }

    @Test
    void loginWorksForCustomersAndEmployees() throws Exception {
        registerCustomer("ana@mail.com");

        assertThat(claims(login(" ANA@mail.com ", "secret123"))).contains("\"account_type\":\"CUSTOMER\"");
        assertThat(claims(adminToken())).contains("\"account_type\":\"EMPLOYEE\"");
    }

    @Test
    void loginRejectsBadCredentialsWithoutSayingWhichPartIsWrong() throws Exception {
        registerCustomer("ana@mail.com");

        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"identifier\":\"ana@mail.com\",\"password\":\"wrong-pass\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("Invalid identifier or password"));
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"identifier\":\"nobody@mail.com\",\"password\":\"wrong-pass\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("Invalid identifier or password"));
    }

    @Test
    void logoutRevokesTheToken() throws Exception {
        String token = registerCustomer("ana@mail.com");

        mvc.perform(post("/api/auth/logout").header("Authorization", bearer(token))).andExpect(status().isNoContent());
        mvc.perform(post("/api/auth/logout").header("Authorization", bearer(token))).andExpect(status().isUnauthorized());
    }

    @Test
    void logoutRequiresAToken() throws Exception {
        mvc.perform(post("/api/auth/logout")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/auth/logout").header("Authorization", "Bearer not-a-jwt")).andExpect(status().isUnauthorized());
    }
}
