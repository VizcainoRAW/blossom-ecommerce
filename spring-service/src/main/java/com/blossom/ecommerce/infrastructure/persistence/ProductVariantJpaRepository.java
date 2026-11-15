package com.blossom.ecommerce.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;

public interface ProductVariantJpaRepository extends JpaRepository<ProductVariantEntity, Long> {

    @Query("SELECT v FROM ProductVariantEntity v JOIN FETCH v.product p WHERE v.id IN :ids AND p.active = true")
    List<ProductVariantEntity> findOrderable(Collection<Long> ids);

    @Modifying
    @Query("UPDATE ProductVariantEntity v SET v.stock = v.stock - :quantity WHERE v.id = :id AND v.stock >= :quantity")
    int decreaseStock(Long id, int quantity);

    @Modifying
    @Query("UPDATE ProductVariantEntity v SET v.stock = v.stock + :quantity WHERE v.id = :id")
    int increaseStock(Long id, int quantity);
}
