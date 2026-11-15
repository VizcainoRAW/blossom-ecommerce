package com.blossom.ecommerce.infrastructure.web;

import com.blossom.ecommerce.domain.exception.DomainException;
import com.blossom.ecommerce.domain.exception.account.IdentifierAlreadyRegisteredException;
import com.blossom.ecommerce.domain.exception.account.InvalidCredentialsException;
import com.blossom.ecommerce.domain.exception.category.CategoryConflictException;
import com.blossom.ecommerce.domain.exception.category.CategoryNotFoundException;
import com.blossom.ecommerce.domain.exception.order.OrderConflictException;
import com.blossom.ecommerce.domain.exception.order.OrderNotFoundException;
import com.blossom.ecommerce.domain.exception.product.ProductNotFoundException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex, HttpHeaders headers,
                                                                  HttpStatusCode status, WebRequest request) {
        ProblemDetail problem = ex.getBody();
        problem.setProperty("errors", ex.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(FieldError::getField,
                        e -> String.valueOf(e.getDefaultMessage()), (a, b) -> a + "; " + b)));
        return handleExceptionInternal(ex, problem, headers, status, request);
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    ProblemDetail invalidCredentials(InvalidCredentialsException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, e.getMessage());
    }

    @ExceptionHandler(IdentifierAlreadyRegisteredException.class)
    ProblemDetail alreadyRegistered(IdentifierAlreadyRegisteredException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, e.getMessage());
    }

    @ExceptionHandler(ProductNotFoundException.class)
    ProblemDetail productNotFound(ProductNotFoundException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler(CategoryNotFoundException.class)
    ProblemDetail categoryNotFound(CategoryNotFoundException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler(CategoryConflictException.class)
    ProblemDetail categoryConflict(CategoryConflictException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, e.getMessage());
    }

    @ExceptionHandler(OrderNotFoundException.class)
    ProblemDetail orderNotFound(OrderNotFoundException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler(OrderConflictException.class)
    ProblemDetail orderConflict(OrderConflictException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, e.getMessage());
    }

    /** Unique/foreign-key violations, e.g. a SKU already used by another product. */
    @ExceptionHandler(DataIntegrityViolationException.class)
    ProblemDetail dataIntegrity(DataIntegrityViolationException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, "The request conflicts with existing data");
    }

    @ExceptionHandler(DomainException.class)
    ProblemDetail domain(DomainException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, e.getMessage());
    }
}
