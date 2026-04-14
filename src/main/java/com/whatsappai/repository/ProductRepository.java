package com.whatsappai.repository;

import com.whatsappai.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductRepository extends JpaRepository<Product, UUID> {
    Optional<Product> findByIdAndBusinessId(UUID id, UUID businessId);
    
    List<Product> findByNameAndBusinessId(String name, UUID businessId);

    Page<Product> findByBusinessId(UUID businessId, Pageable pageable);

    List<Product> findByBusinessIdAndIsActiveTrue(UUID businessId);

    @Query("SELECT p FROM Product p WHERE p.businessId = :biz AND p.isActive = true AND p.stockQty > 0 " +
           "AND LOWER(p.name) LIKE LOWER(CONCAT('%', :term, '%'))")
    List<Product> findActiveByNameContaining(@Param("biz") UUID businessId, @Param("term") String term, Pageable pageable);

    @Query("SELECT p FROM Product p WHERE p.businessId = :biz AND p.category = :cat AND p.isActive = true AND p.stockQty > 0 AND p.id <> :excludeId")
    List<Product> findAlternativesByCategory(@Param("biz") UUID businessId, @Param("cat") String category, @Param("excludeId") UUID excludeId, Pageable pageable);

    @Modifying
    @Query("UPDATE Product p SET p.isActive = false WHERE p.id = :id AND p.businessId = :biz")
    void softDelete(@Param("id") UUID id, @Param("biz") UUID businessId);

    @Modifying
    @Query("UPDATE Product p SET p.stockQty = p.stockQty - :qty WHERE p.id = :id AND p.stockQty >= :qty")
    int decrementStock(@Param("id") UUID id, @Param("qty") int qty);
}
