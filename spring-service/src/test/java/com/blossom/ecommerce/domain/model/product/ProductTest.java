package com.blossom.ecommerce.domain.model.product;

import com.blossom.ecommerce.domain.exception.product.InvalidProductException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProductTest {

    static ProductVariant variant(String color, String size, String sku) {
        return new ProductVariant(null, color, size, sku, new BigDecimal("10.00"), 5);
    }

    @Test
    void createBuildsActiveProduct() {
        Product p = Product.create(1L, "  Tee ", "Soft", null, List.of(variant("Black", "M", "T-1")));

        assertThat(p.getId()).isNull();
        assertThat(p.isActive()).isTrue();
        assertThat(p.getName()).isEqualTo("Tee");
        assertThat(p.getCategoryId()).isEqualTo(1L);
        assertThat(p.getDescription()).isEqualTo("Soft");
        assertThat(p.getImageUrls()).isEmpty();
        assertThat(p.getVariants()).hasSize(1);
    }

    @Test
    void requiresCategoryNameAndVariants() {
        List<ProductVariant> one = List.of(variant("Black", "M", "T-1"));

        assertThatThrownBy(() -> Product.create(null, "Tee", null, null, one)).hasMessage("Category required");
        assertThatThrownBy(() -> Product.create(1L, " ", null, null, one)).hasMessage("Name required");
        assertThatThrownBy(() -> Product.create(1L, "Tee", null, null, List.of())).hasMessage("At least one variant required");
        assertThatThrownBy(() -> Product.create(1L, "Tee", null, null, null)).isInstanceOf(InvalidProductException.class);
    }

    @Test
    void rejectsDuplicateSku() {
        assertThatThrownBy(() -> Product.create(1L, "Tee", null, null,
                List.of(variant("Black", "M", "T-1"), variant("White", "M", "T-1"))))
                .hasMessage("Duplicate SKU T-1");
    }

    @Test
    void rejectsDuplicateColorSizeIgnoringCase() {
        assertThatThrownBy(() -> Product.create(1L, "Tee", null, null,
                List.of(variant("Black", "M", "T-1"), variant("black", "m", "T-2"))))
                .hasMessage("Duplicate variant black/m");
    }

    @Test
    void updateReplacesDetailsAndDeactivateHides() {
        Product p = Product.restore(9L, 1L, "Old", null, List.of(), true, List.of(variant("Black", "M", "T-1")));

        p.update(2L, "New", "Desc", List.of("https://img"), List.of(variant("Red", "L", "T-9")));
        p.deactivate();

        assertThat(p.getId()).isEqualTo(9L);
        assertThat(p.getCategoryId()).isEqualTo(2L);
        assertThat(p.getName()).isEqualTo("New");
        assertThat(p.getImageUrls()).containsExactly("https://img");
        assertThat(p.getVariants()).extracting(ProductVariant::sku).containsExactly("T-9");
        assertThat(p.isActive()).isFalse();
        assertThat(p.toString()).isEqualTo("Product(9/New)");
    }

    @Test
    void variantTrimsAndValidates() {
        ProductVariant v = new ProductVariant(1L, " Black ", " M ", " T-1 ", BigDecimal.ZERO, 0);
        assertThat(v.color()).isEqualTo("Black");
        assertThat(v.size()).isEqualTo("M");
        assertThat(v.sku()).isEqualTo("T-1");

        BigDecimal price = BigDecimal.ONE;
        assertThatThrownBy(() -> new ProductVariant(null, " ", "M", "S", price, 1)).hasMessage("Variant color required");
        assertThatThrownBy(() -> new ProductVariant(null, "B", null, "S", price, 1)).hasMessage("Variant size required");
        assertThatThrownBy(() -> new ProductVariant(null, "B", "M", "", price, 1)).hasMessage("Variant SKU required");
        assertThatThrownBy(() -> new ProductVariant(null, "B", "M", "S", null, 1)).hasMessage("Variant price must be >= 0");
        assertThatThrownBy(() -> new ProductVariant(null, "B", "M", "S", new BigDecimal("-1"), 1))
                .hasMessage("Variant price must be >= 0");
        assertThatThrownBy(() -> new ProductVariant(null, "B", "M", "S", price, -1)).hasMessage("Variant stock must be >= 0");
    }
}
