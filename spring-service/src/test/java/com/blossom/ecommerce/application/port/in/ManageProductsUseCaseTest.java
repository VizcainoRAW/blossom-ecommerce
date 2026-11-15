package com.blossom.ecommerce.application.port.in;

import com.blossom.ecommerce.application.port.out.CategoryRepository;
import com.blossom.ecommerce.application.port.out.PageResult;
import com.blossom.ecommerce.application.port.out.ProductRepository;
import com.blossom.ecommerce.domain.exception.product.InvalidProductException;
import com.blossom.ecommerce.domain.exception.product.ProductNotFoundException;
import com.blossom.ecommerce.domain.model.product.Product;
import com.blossom.ecommerce.domain.model.product.ProductVariant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ManageProductsUseCaseTest {

    @Mock ProductRepository products;
    @Mock CategoryRepository categories;
    ManageProductsUseCase useCase;

    final List<ProductVariant> variants = List.of(new ProductVariant(null, "Black", "M", "T-1", BigDecimal.TEN, 3));

    @BeforeEach
    void setUp() {
        useCase = new ManageProductsUseCase(products, categories);
    }

    @Test
    void createRequiresExistingCategory() {
        when(categories.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> useCase.create(99L, "Tee", null, null, variants))
                .isInstanceOf(InvalidProductException.class).hasMessage("Category 99 does not exist");
        verify(products, never()).save(any());
    }

    @Test
    void createSaves() {
        when(categories.existsById(1L)).thenReturn(true);
        when(products.save(any())).thenAnswer(inv -> inv.getArgument(0));

        assertThat(useCase.create(1L, "Tee", null, null, variants).getName()).isEqualTo("Tee");
    }

    @Test
    void getMissingProductFails() {
        when(products.findActiveById(5L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.get(5L)).isInstanceOf(ProductNotFoundException.class).hasMessage("Product 5 not found");
    }

    @Test
    void updateChangesExistingProduct() {
        when(products.findActiveById(5L)).thenReturn(Optional.of(Product.restore(5L, 1L, "Old", null, List.of(), true, variants)));
        when(categories.existsById(2L)).thenReturn(true);
        when(products.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Product updated = useCase.update(5L, 2L, "New", null, null, variants);

        assertThat(updated.getName()).isEqualTo("New");
        assertThat(updated.getCategoryId()).isEqualTo(2L);
    }

    @Test
    void deleteDeactivatesInsteadOfRemoving() {
        when(products.findActiveById(5L)).thenReturn(Optional.of(Product.restore(5L, 1L, "Tee", null, List.of(), true, variants)));

        useCase.delete(5L);

        ArgumentCaptor<Product> saved = ArgumentCaptor.forClass(Product.class);
        verify(products).save(saved.capture());
        assertThat(saved.getValue().isActive()).isFalse();
    }

    @Test
    void searchDelegates() {
        ProductSearchCriteria criteria = new ProductSearchCriteria(null, null, null, null, 0, 20);
        PageResult<Product> page = new PageResult<>(List.of(), 0, 20, 0);
        when(products.searchActive(criteria)).thenReturn(page);

        assertThat(useCase.search(criteria)).isSameAs(page);
    }

    @Test
    void searchCriteriaNormalizesPagingAndName() {
        ProductSearchCriteria c = new ProductSearchCriteria("  ", null, null, null, -3, 1000);
        assertThat(c.name()).isNull();
        assertThat(c.page()).isZero();
        assertThat(c.size()).isEqualTo(ProductSearchCriteria.MAX_PAGE_SIZE);

        assertThat(new ProductSearchCriteria(" tee ", null, null, null, 0, 0).name()).isEqualTo("tee");
        assertThat(new ProductSearchCriteria(null, null, null, null, 0, 0).size()).isEqualTo(1);
    }

    @Test
    void pageResultMapsItems() {
        PageResult<String> mapped = new PageResult<>(List.of(1, 2), 1, 2, 10).map(String::valueOf);

        assertThat(mapped.items()).containsExactly("1", "2");
        assertThat(mapped.page()).isEqualTo(1);
        assertThat(mapped.totalItems()).isEqualTo(10);
    }
}
