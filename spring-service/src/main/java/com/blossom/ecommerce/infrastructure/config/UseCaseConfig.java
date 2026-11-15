package com.blossom.ecommerce.infrastructure.config;

import com.blossom.ecommerce.application.port.in.LoginUseCase;
import com.blossom.ecommerce.application.port.in.ManageCategoriesUseCase;
import com.blossom.ecommerce.application.port.out.CategoryRepository;
import com.blossom.ecommerce.application.port.in.ManageProductsUseCase;
import com.blossom.ecommerce.application.port.out.ProductRepository;
import com.blossom.ecommerce.application.port.in.RegisterCustomerUseCase;
import com.blossom.ecommerce.application.port.out.AccountRepository;
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
    ManageProductsUseCase manageProductsUseCase(ProductRepository products, CategoryRepository categories) {
        return new ManageProductsUseCase(products, categories);
    }

    @Bean
    ManageCategoriesUseCase manageCategoriesUseCase(CategoryRepository repository) {
        return new ManageCategoriesUseCase(repository);
    }

    @Bean
    LoginUseCase loginUseCase(AccountRepository repository, PasswordEncoderPort encoder) {
        return new LoginUseCase(repository, encoder);
    }
}
