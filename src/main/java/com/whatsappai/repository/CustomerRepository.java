package com.whatsappai.repository;

import com.whatsappai.entity.Customer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;
import java.util.UUID;

public interface CustomerRepository extends JpaRepository<Customer, UUID> {
    Optional<Customer> findByBusinessIdAndPhone(UUID businessId, String phone);
    Page<Customer> findByBusinessId(UUID businessId, Pageable pageable);

    @Modifying
    @Query("UPDATE Customer c SET c.name = :name WHERE c.businessId = :biz AND c.phone = :phone")
    void updateName(@Param("biz") UUID businessId, @Param("phone") String phone, @Param("name") String name);

    @Modifying
    @Query("UPDATE Customer c SET c.blocked = :blocked WHERE c.businessId = :biz AND c.phone = :phone")
    void updateBlocked(@Param("biz") UUID businessId, @Param("phone") String phone, @Param("blocked") boolean blocked);

    @Modifying
    @Query("UPDATE Customer c SET c.totalOrders = c.totalOrders + 1 WHERE c.id = :id")
    void incrementTotalOrders(@Param("id") UUID customerId);

    @Modifying
    @Query("UPDATE Customer c SET c.requiresHuman = :requiresHuman WHERE c.businessId = :biz AND c.phone = :phone")
    void updateRequiresHuman(@Param("biz") UUID businessId, @Param("phone") String phone, @Param("requiresHuman") boolean requiresHuman);

    @Modifying
    @Query("UPDATE Customer c SET c.requiresHuman = :requiresHuman, c.requiresHumanReason = :reason WHERE c.businessId = :biz AND c.phone = :phone")
    void updateRequiresHumanWithReason(@Param("biz") UUID businessId, @Param("phone") String phone, @Param("requiresHuman") boolean requiresHuman, @Param("reason") String reason);
}
