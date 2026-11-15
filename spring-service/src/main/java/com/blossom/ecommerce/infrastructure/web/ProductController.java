package com.blossom.ecommerce.infrastructure.web;

import com.blossom.ecommerce.application.port.in.ManageProductsUseCase;
import com.blossom.ecommerce.application.port.in.ProductSearchCriteria;
import com.blossom.ecommerce.application.port.out.PageResult;
import com.blossom.ecommerce.infrastructure.web.dto.ProductRequest;
import com.blossom.ecommerce.infrastructure.web.dto.ProductResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ManageProductsUseCase products;

    public ProductController(ManageProductsUseCase products) {
        this.products = products;
    }

    @GetMapping
    public PageResult<ProductResponse> search(@RequestParam(required = false) String name,
                                              @RequestParam(required = false) Long categoryId,
                                              @RequestParam(required = false) BigDecimal minPrice,
                                              @RequestParam(required = false) BigDecimal maxPrice,
                                              @RequestParam(defaultValue = "0") int page,
                                              @RequestParam(defaultValue = "20") int size) {
        return products.search(new ProductSearchCriteria(name, categoryId, minPrice, maxPrice, page, size))
                .map(ProductResponse::from);
    }

    @GetMapping("/{id}")
    public ProductResponse get(@PathVariable Long id) {
        return ProductResponse.from(products.get(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProductResponse create(@Valid @RequestBody ProductRequest request) {
        return ProductResponse.from(products.create(request.categoryId(), request.name(), request.description(),
                request.imageUrls(), request.toVariants()));
    }

    @PutMapping("/{id}")
    public ProductResponse update(@PathVariable Long id, @Valid @RequestBody ProductRequest request) {
        return ProductResponse.from(products.update(id, request.categoryId(), request.name(), request.description(),
                request.imageUrls(), request.toVariants()));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        products.delete(id);
    }
}
