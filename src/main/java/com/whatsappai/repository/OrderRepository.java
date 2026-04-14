package com.whatsappai.repository;

import com.whatsappai.entity.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrderRepository extends JpaRepository<Order, UUID> {
    Page<Order> findByBusinessId(UUID businessId, Pageable pageable);
    Page<Order> findByBusinessIdAndStatus(UUID businessId, String status, Pageable pageable);

    Optional<Order> findByIdAndBusinessId(UUID id, UUID businessId);

    List<Order> findByBusinessIdAndCustomerPhoneOrderByCreatedAtDesc(UUID businessId, String phone);

    @Query("SELECT o FROM Order o WHERE o.businessId = :biz AND o.customerPhone = :phone AND o.status = 'DELIVERED' ORDER BY o.createdAt DESC")
    List<Order> findLastDelivered(@Param("biz") UUID businessId, @Param("phone") String phone, Pageable pageable);

    @Modifying
    @Query("UPDATE Order o SET o.status = :status WHERE o.id = :id AND o.businessId = :biz")
    void updateStatus(@Param("id") UUID id, @Param("biz") UUID businessId, @Param("status") String status);

    @Query(value = "SELECT COUNT(*) FROM orders WHERE business_id = :biz AND created_at >= NOW() - CAST(:days || ' days' AS INTERVAL)", nativeQuery = true)
    long countByBusinessIdAndDays(@Param("biz") UUID businessId, @Param("days") int days);

    @Query(value = "SELECT COALESCE(SUM(total_amount), 0) FROM orders WHERE business_id = :biz AND status IN ('CONFIRMED','DELIVERED') AND created_at >= NOW() - CAST(:days || ' days' AS INTERVAL)", nativeQuery = true)
    java.math.BigDecimal sumRevenueByDays(@Param("biz") UUID businessId, @Param("days") int days);
}
