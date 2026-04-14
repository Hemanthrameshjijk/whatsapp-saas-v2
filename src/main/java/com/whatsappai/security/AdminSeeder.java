package com.whatsappai.security;

import com.whatsappai.entity.Business;
import com.whatsappai.entity.AppUser;
import com.whatsappai.repository.BusinessRepository;
import com.whatsappai.repository.AppUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class AdminSeeder implements CommandLineRunner {

    private final AppUserRepository userRepository;
    private final BusinessRepository businessRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        if (userRepository.count() == 0) {
            List<Business> businesses = businessRepository.findAll();
            if (!businesses.isEmpty()) {
                Business firstBiz = businesses.get(0);
                AppUser admin = new AppUser();
                admin.setBusinessId(firstBiz.getId());
                // Defaults to 'admin' / 'admin'
                admin.setEmail("admin");
                admin.setPasswordHash(passwordEncoder.encode("admin"));
                admin.setRole("ADMIN");
                userRepository.save(admin);
                log.info("====== AUTO-SEEDED ADMIN USER ======");
                log.info("Username: admin");
                log.info("Password: admin");
                log.info("Business ID: {}", firstBiz.getId());
                log.info("====================================");
            }
        }
    }
}
