package com.whatsappai.service;

import com.whatsappai.entity.AISettings;
import com.whatsappai.entity.AppUser;
import com.whatsappai.entity.Business;

import com.whatsappai.qdrant.QdrantClient;
import com.whatsappai.repository.AISettingsRepository;
import com.whatsappai.repository.AppUserRepository;
import com.whatsappai.repository.BusinessRepository;
import com.whatsappai.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class AuthService {

    private final BusinessRepository businessRepository;
    private final AISettingsRepository aiSettingsRepository;
    private final AppUserRepository userRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final QdrantClient qdrantClient;

    /** 4-step transactional registration per spec.
     *  Qdrant collection creation is non-blocking and logged on failure. */
    @Transactional
    public String register(String businessName, String whatsappNumber, String email, String password) {
        if (userRepository.existsByEmail(email))
            throw new IllegalArgumentException("Email already registered: " + email);

        // Step 1: Create business
        Business business = businessRepository.save(Business.builder()
            .name(businessName)
            .whatsappNumber(whatsappNumber)
            .build());

        // Step 2: Create default ai_settings
        aiSettingsRepository.save(AISettings.builder()
            .businessId(business.getId())
            .build());

        // Step 3: Create user with BCrypt(12) hashed password, role=ADMIN
        AppUser user = userRepository.save(AppUser.builder()
            .businessId(business.getId())
            .email(email)
            .passwordHash(passwordEncoder.encode(password))
            .role("ADMIN")
            .build());

        // Step 4: Create Qdrant collection — non-fatal if Qdrant is down
        try {
            qdrantClient.createCollection("memories_" + business.getId(), 384);
        } catch (Exception e) {
            log.error("Qdrant collection creation failed for {} — app registration still succeeds: {}",
                business.getId(), e.getMessage());
        }

        return jwtService.generateToken(user);
    }

    public String login(String email, String password) {
        AppUser user = userRepository.findByEmail(email)
            .orElseThrow(() -> new UsernameNotFoundException("User not found: " + email));
        if (!passwordEncoder.matches(password, user.getPasswordHash()))
            throw new BadCredentialsException("Invalid password");
        return jwtService.generateToken(user);
    }
}
