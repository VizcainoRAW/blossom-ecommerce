package com.blossom.ecommerce.application.port.out;

public interface PasswordEncoderPort {

    String encode(String password);

    boolean matches(String rawPassword, String passwordHash);
}
