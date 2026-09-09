package com.ecommerce.backend.config;

import com.ecommerce.backend.entity.User;
import com.ecommerce.backend.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class CustomUserDetailsService implements UserDetailsService {

    @Autowired
    private UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username)
            throws UsernameNotFoundException {

        log.debug("Loading user by username (email): {}", username);

        try {
            User user = userRepository.findActiveByEmail(username).orElseThrow(() -> {
                        log.warn("User not found with email: {}", username);
                        return new UsernameNotFoundException("User not found");
                    });

            log.debug(
                    "Successfully loaded user with email: {} and id: {}",
                    username,
                    user.getUserId()
            );

            return new CustomUserDetails(user);

        } catch (UsernameNotFoundException e) {
            throw e;
        } catch (Exception e) {
            log.error(
                    "Unexpected error while loading user by username: {}",
                    username,
                    e
            );
            throw e;
        }
    }

    public boolean doesUserExist(String email) {
        return userRepository.existsActiveByEmail(email);
    }
}