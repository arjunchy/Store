package com.ecommerce.backend.config;

import com.ecommerce.backend.entity.User;
import com.ecommerce.backend.enums.UserRole;
import com.ecommerce.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        String adminEmail = "ecommerce@gmail.com";

        if (!userRepository.existsActiveByEmail(adminEmail)) {
            User admin = User.builder()
                    .username("admin")
                    .email(adminEmail)
                    .passwordHash(passwordEncoder.encode("ecommerce@gmail"))
                    .userRole(UserRole.ADMIN)
                    .build();

            userRepository.save(admin);
            log.info("Admin user created with email: {}", adminEmail);
        } else {
            log.info("Admin user already exists, skipping creation.");
        }
    }
}
