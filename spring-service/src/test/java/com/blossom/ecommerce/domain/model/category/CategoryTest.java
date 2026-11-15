package com.blossom.ecommerce.domain.model.category;

import com.blossom.ecommerce.domain.exception.category.InvalidCategoryException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CategoryTest {

    @Test
    void createTrimsName() {
        Category c = Category.create("  Hats ");

        assertThat(c.getId()).isNull();
        assertThat(c.getName()).isEqualTo("Hats");
    }

    @Test
    void renameValidates() {
        Category c = Category.restore(3L, "Hats");

        assertThatThrownBy(() -> c.rename(" ")).isInstanceOf(InvalidCategoryException.class).hasMessage("Name required");
        assertThatThrownBy(() -> c.rename(null)).isInstanceOf(InvalidCategoryException.class);
        assertThatThrownBy(() -> c.rename("x".repeat(151))).hasMessage("Name must be at most 150 characters");

        c.rename("Caps");
        assertThat(c.getName()).isEqualTo("Caps");
        assertThat(c.toString()).isEqualTo("Category(3/Caps)");
    }
}
