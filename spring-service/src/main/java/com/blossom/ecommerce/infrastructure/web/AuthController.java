package com.blossom.ecommerce.infrastructure.web;

import com.blossom.ecommerce.application.port.in.LoginUseCase;
import com.blossom.ecommerce.application.port.in.RegisterCustomerUseCase;
import com.blossom.ecommerce.domain.model.user.Customer;
import com.blossom.ecommerce.infrastructure.security.JwtTokenService;
import com.blossom.ecommerce.infrastructure.security.RevokedTokenStore;
import com.blossom.ecommerce.infrastructure.web.dto.JwtResponse;
import com.blossom.ecommerce.infrastructure.web.dto.LoginRequest;
import com.blossom.ecommerce.infrastructure.web.dto.RegisterRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final RegisterCustomerUseCase registerCustomer;
    private final LoginUseCase login;
    private final JwtTokenService tokens;
    private final RevokedTokenStore revokedTokens;

    public AuthController(RegisterCustomerUseCase registerCustomer, LoginUseCase login,
                          JwtTokenService tokens, RevokedTokenStore revokedTokens) {
        this.registerCustomer = registerCustomer;
        this.login = login;
        this.tokens = tokens;
        this.revokedTokens = revokedTokens;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public JwtResponse register(@Valid @RequestBody RegisterRequest request) {
        Customer customer = registerCustomer.registerCustomer(request.firstName(), request.lastName(),
                request.identifier(), request.password(), request.documentType(), request.documentNumber());
        return tokens.issue(customer);
    }

    @PostMapping("/login")
    public JwtResponse login(@Valid @RequestBody LoginRequest request) {
        return tokens.issue(this.login.login(request.identifier(), request.password()));
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(@AuthenticationPrincipal Jwt jwt) {
        revokedTokens.revoke(jwt.getId(), jwt.getExpiresAt());
    }
}
