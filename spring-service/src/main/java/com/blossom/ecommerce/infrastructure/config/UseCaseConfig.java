package com.blossom.ecommerce.infrastructure.config;

import com.blossom.ecommerce.application.port.in.LoginCustomerUseCase;
import com.blossom.ecommerce.application.port.in.RegisterCustomerUseCase;
import com.blossom.ecommerce.application.port.out.CustomerRepository;
import com.blossom.ecommerce.application.port.out.PasswordEncoderPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class UseCaseConfig {

    @Bean
    RegisterCustomerUseCase registerCustomerUseCase(CustomerRepository repository, PasswordEncoderPort encoder) {
        return new RegisterCustomerUseCase(repository, encoder);
    }

    @Bean
    LoginCustomerUseCase loginCustomerUseCase(CustomerRepository repository, PasswordEncoderPort encoder) {
        return new LoginCustomerUseCase(repository, encoder);
    }
}
