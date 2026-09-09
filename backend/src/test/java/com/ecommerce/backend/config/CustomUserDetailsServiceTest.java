package com.ecommerce.backend.config;

import com.ecommerce.backend.entity.User;
import com.ecommerce.backend.enums.UserRole;
import com.ecommerce.backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CustomUserDetailsService service;

    private User activeUser;

    @BeforeEach
    void setUp() {
        activeUser = User.builder()
                .userId("uuid-1")
                .username("john")
                .email("john@example.com")
                .passwordHash("hashed")
                .userRole(UserRole.USER)
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    void loadUserByUsername_success_activeUser() {
        when(userRepository.findActiveByEmail("john@example.com")).thenReturn(Optional.of(activeUser));

        var userDetails = service.loadUserByUsername("john@example.com");

        assertThat(userDetails.getUsername()).isEqualTo("john@example.com");
        assertThat(userDetails.getPassword()).isEqualTo("hashed");
        assertThat(userDetails.getAuthorities()).anyMatch(a -> a.getAuthority().equals("ROLE_USER"));
    }

    @Test
    void loadUserByUsername_success_adminRole() {
        User admin = User.builder().userId("2").username("admin").email("admin@example.com").passwordHash("h").userRole(UserRole.ADMIN).build();
        when(userRepository.findActiveByEmail("admin@example.com")).thenReturn(Optional.of(admin));

        var userDetails = service.loadUserByUsername("admin@example.com");

        assertThat(userDetails.getAuthorities()).anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }

    @Test
    void loadUserByUsername_notFound_throwsUsernameNotFoundException() {
        when(userRepository.findActiveByEmail("missing@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.loadUserByUsername("missing@example.com"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("User not found");
    }

    @Test
    void loadUserByUsername_softDeleted_throws() {
        when(userRepository.findActiveByEmail("deleted@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.loadUserByUsername("deleted@example.com"))
                .isInstanceOf(UsernameNotFoundException.class);
    }

    @Test
    void loadUserByUsername_delegatesToActiveQueryOnly() {
        when(userRepository.findActiveByEmail("john@example.com")).thenReturn(Optional.of(activeUser));

        service.loadUserByUsername("john@example.com");

        // verify findActiveByEmail is used, not findByEmail
        org.mockito.Mockito.verify(userRepository).findActiveByEmail("john@example.com");
        org.mockito.Mockito.verify(userRepository, org.mockito.Mockito.never()).findByEmail(org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void doesUserExist_trueWhenActive() {
        when(userRepository.existsActiveByEmail("john@example.com")).thenReturn(true);

        assertThat(service.doesUserExist("john@example.com")).isTrue();
    }

    @Test
    void doesUserExist_falseWhenMissingOrDeleted() {
        when(userRepository.existsActiveByEmail("ghost@example.com")).thenReturn(false);

        assertThat(service.doesUserExist("ghost@example.com")).isFalse();
    }

    @Test
    void loadUserByUsername_usernameIsEmail() {
        when(userRepository.findActiveByEmail("john@example.com")).thenReturn(Optional.of(activeUser));

        var details = service.loadUserByUsername("john@example.com");

        // CustomUserDetails.getUsername() should return email (principal)
        assertThat(details.getUsername()).isEqualTo(activeUser.getEmail());
        assertThat(details.getUsername()).isNotEqualTo(activeUser.getUsername());
    }
}
