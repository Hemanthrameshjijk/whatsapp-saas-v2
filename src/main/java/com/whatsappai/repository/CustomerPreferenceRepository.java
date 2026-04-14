package com.whatsappai.repository;

import com.whatsappai.entity.CustomerPreference;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CustomerPreferenceRepository extends JpaRepository<CustomerPreference, UUID> {
    Optional<CustomerPreference> findByBusinessIdAndCustomerPhoneAndPrefKey(UUID businessId, String phone, String key);
    List<CustomerPreference> findByBusinessIdAndCustomerPhone(UUID businessId, String phone);

    @Modifying
    @Query("DELETE FROM CustomerPreference cp WHERE cp.businessId = :biz AND cp.customerPhone = :phone")
    void deleteByBusinessIdAndCustomerPhone(@Param("biz") UUID businessId, @Param("phone") String phone);
}
