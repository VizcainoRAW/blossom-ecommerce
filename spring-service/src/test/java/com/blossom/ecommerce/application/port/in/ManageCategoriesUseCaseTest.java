package com.blossom.ecommerce.application.port.in;

import com.blossom.ecommerce.application.port.out.CategoryRepository;
import com.blossom.ecommerce.domain.exception.category.CategoryConflictException;
import com.blossom.ecommerce.domain.exception.category.CategoryNotFoundException;
import com.blossom.ecommerce.domain.model.category.Category;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ManageCategoriesUseCaseTest {

    @Mock CategoryRepository repository;
    ManageCategoriesUseCase useCase;

    @BeforeEach
    void setUp() {
        useCase = new ManageCategoriesUseCase(repository);
    }

    @Test
    void createSavesNewCategory() {
        when(repository.findByNameIgnoreCase("Hats")).thenReturn(Optional.empty());
        when(repository.save(any())).thenReturn(Category.restore(6L, "Hats"));

        assertThat(useCase.create(" Hats ").getId()).isEqualTo(6L);
    }

    @Test
    void createRejectsDuplicateName() {
        when(repository.findByNameIgnoreCase("Hats")).thenReturn(Optional.of(Category.restore(6L, "hats")));

        assertThatThrownBy(() -> useCase.create("Hats")).isInstanceOf(CategoryConflictException.class);
        verify(repository, never()).save(any());
    }

    @Test
    void renameToOwnNameInOtherCaseIsAllowed() {
        when(repository.findById(6L)).thenReturn(Optional.of(Category.restore(6L, "Hats")));
        when(repository.findByNameIgnoreCase("HATS")).thenReturn(Optional.of(Category.restore(6L, "Hats")));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        assertThat(useCase.rename(6L, "HATS").getName()).isEqualTo("HATS");
    }

    @Test
    void renameToAnotherCategorysNameConflicts() {
        when(repository.findById(6L)).thenReturn(Optional.of(Category.restore(6L, "Hats")));
        when(repository.findByNameIgnoreCase("Shoes")).thenReturn(Optional.of(Category.restore(4L, "Shoes")));

        assertThatThrownBy(() -> useCase.rename(6L, "Shoes")).isInstanceOf(CategoryConflictException.class);
    }

    @Test
    void getAndListDelegate() {
        when(repository.findById(99L)).thenReturn(Optional.empty());
        when(repository.findAll()).thenReturn(List.of(Category.restore(1L, "Shirts")));

        assertThatThrownBy(() -> useCase.get(99L)).isInstanceOf(CategoryNotFoundException.class);
        assertThat(useCase.list()).hasSize(1);
    }

    @Test
    void deleteOnlyWhenUnused() {
        when(repository.findById(1L)).thenReturn(Optional.of(Category.restore(1L, "Shirts")));
        when(repository.findById(6L)).thenReturn(Optional.of(Category.restore(6L, "Hats")));
        when(repository.hasProducts(1L)).thenReturn(true);
        when(repository.hasProducts(6L)).thenReturn(false);

        assertThatThrownBy(() -> useCase.delete(1L)).isInstanceOf(CategoryConflictException.class);
        useCase.delete(6L);

        verify(repository, never()).deleteById(1L);
        verify(repository).deleteById(6L);
    }
}
