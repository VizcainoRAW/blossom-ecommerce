package com.blossom.ecommerce.infrastructure.web;

import com.blossom.ecommerce.application.port.in.ManageCategoriesUseCase;
import com.blossom.ecommerce.infrastructure.web.dto.CategoryRequest;
import com.blossom.ecommerce.infrastructure.web.dto.CategoryResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
public class CategoryController {

    private final ManageCategoriesUseCase categories;

    public CategoryController(ManageCategoriesUseCase categories) {
        this.categories = categories;
    }

    @GetMapping
    public List<CategoryResponse> list() {
        return categories.list().stream().map(CategoryResponse::from).toList();
    }

    @GetMapping("/{id}")
    public CategoryResponse get(@PathVariable Long id) {
        return CategoryResponse.from(categories.get(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CategoryResponse create(@Valid @RequestBody CategoryRequest request) {
        return CategoryResponse.from(categories.create(request.name()));
    }

    @PutMapping("/{id}")
    public CategoryResponse rename(@PathVariable Long id, @Valid @RequestBody CategoryRequest request) {
        return CategoryResponse.from(categories.rename(id, request.name()));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        categories.delete(id);
    }
}
