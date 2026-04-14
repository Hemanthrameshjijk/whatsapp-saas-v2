package com.whatsappai.service;

import com.whatsappai.entity.Business;
import com.whatsappai.repository.BusinessRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import com.whatsappai.entity.AppUser;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
@RequiredArgsConstructor
public class TenantResolver {

    @Value("${redis.business-config-ttl-minutes:10}")
    private long cacheTtl;

    private final BusinessRepository businessRepository;
    private final RedisTemplate<String, Object> redisTemplate;

    public Optional<Business> resolve(String whatsappNumber) {
        String cacheKey = "biz:wa:" + whatsappNumber;
        try {
            Object cached = redisTemplate.opsForValue().get(cacheKey);
            if (cached instanceof Business biz) return Optional.of(biz);
        } catch (Exception e) {
            log.debug("Stale Redis cache for {} — refetching from DB", cacheKey);
            redisTemplate.delete(cacheKey);
        }

        Optional<Business> biz = businessRepository.findByWhatsappNumber(whatsappNumber);
        biz.ifPresent(b -> redisTemplate.opsForValue().set(cacheKey, b, cacheTtl, TimeUnit.MINUTES));
        if (biz.isEmpty()) log.warn("Unknown WA number: {}", whatsappNumber);
        return biz;
    }

    public void evict(String whatsappNumber) {
        redisTemplate.delete("biz:wa:" + whatsappNumber);
    }

    public UUID getBusinessIdFromContext() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof AppUser user) {
            return user.getBusinessId();
        }
        throw new RuntimeException("No business ID found in security context");
    }
}
