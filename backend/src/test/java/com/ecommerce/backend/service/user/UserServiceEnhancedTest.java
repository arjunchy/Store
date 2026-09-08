package com.ecommerce.backend.service.user;

import com.ecommerce.backend.dto.response.UserResponse;
import com.ecommerce.backend.entity.User;
import com.ecommerce.backend.enums.UserRole;
import com.ecommerce.backend.mapper.UserMapper;
import com.ecommerce.backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class UserServiceEnhancedTest {

    @Mock private UserRepository userRepository;
    @Mock private UserMapper userMapper;
    @Mock private org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserServiceImpl userService;

    private User activeUser;
    private User deletedUser;

    @BeforeEach
    void setUp() {
        activeUser = User.builder()
                .userId("user-1")
                .username("john")
                .email("john@example.com")
                .userRole(UserRole.USER)
                .deletedAt(null)
                .createdAt(LocalDateTime.now())
                .build();

        deletedUser = User.builder()
                .userId("user-2")
                .username("deleted")
                .email("deleted@example.com")
                .userRole(UserRole.USER)
                .deletedAt(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .build();

        lenient().when(userMapper.toResponse(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            return new UserResponse(u.getUserId(), u.getUsername(), u.getEmail(), u.getUserRole(), u.getCreatedAt());
        });
    }

    @Test
    void getAllUsers_includeDeleted_false_returnsActive() {
        when(userRepository.findAllActive()).thenReturn(List.of(activeUser));
        List<UserResponse> result = userService.getAllUsers(false);
        assertThat(result).hasSize(1);
        assertThat(result.get(0).id()).isEqualTo("user-1");
        verify(userRepository).findAllActive();
        verify(userRepository, never()).findAllIncludingDeleted();
    }

    @Test
    void getAllUsers_includeDeleted_true_returnsAll() {
        when(userRepository.findAllIncludingDeleted()).thenReturn(List.of(activeUser, deletedUser));
        List<UserResponse> result = userService.getAllUsers(true);
        assertThat(result).hasSize(2);
        verify(userRepository).findAllIncludingDeleted();
    }

    @Test
    void changeUserRole_success() {
        when(userRepository.findByIdIncludingDeleted("user-1")).thenReturn(Optional.of(activeUser));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UserResponse resp = userService.changeUserRole("user-1", UserRole.ADMIN);

        assertThat(resp.userRole()).isEqualTo(UserRole.ADMIN);
        assertThat(activeUser.getUserRole()).isEqualTo(UserRole.ADMIN);
        verify(userRepository).save(activeUser);
    }

    @Test
    void changeUserRole_userNotFound_throws() {
        when(userRepository.findByIdIncludingDeleted("missing")).thenReturn(Optional.empty());
        when(userRepository.findById("missing")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> userService.changeUserRole("missing", UserRole.ADMIN))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("User not found");
    }

    @Test
    void changeUserRole_deactivated_throws() {
        when(userRepository.findByIdIncludingDeleted("user-2")).thenReturn(Optional.of(deletedUser));
        assertThatThrownBy(() -> userService.changeUserRole("user-2", UserRole.ADMIN))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("deactivated");
    }

    @Test
    void changeUserRole_nullRole_throws() {
        assertThatThrownBy(() -> userService.changeUserRole("user-1", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Role is required");
    }

    @Test
    void toggleUserStatus_activate_success() {
        when(userRepository.findByIdIncludingDeleted("user-2")).thenReturn(Optional.of(deletedUser));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UserResponse resp = userService.toggleUserStatus("user-2", true);
        assertThat(deletedUser.getDeletedAt()).isNull();
        verify(userRepository).save(deletedUser);
    }

    @Test
    void toggleUserStatus_activate_alreadyActive_throws() {
        when(userRepository.findByIdIncludingDeleted("user-1")).thenReturn(Optional.of(activeUser));
        assertThatThrownBy(() -> userService.toggleUserStatus("user-1", true))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already active");
    }

    @Test
    void toggleUserStatus_deactivate_success() {
        when(userRepository.findByIdIncludingDeleted("user-1")).thenReturn(Optional.of(activeUser));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        UserResponse resp = userService.toggleUserStatus("user-1", false);
        assertThat(activeUser.getDeletedAt()).isNotNull();
    }

    @Test
    void toggleUserStatus_deactivate_alreadyDeactivated_throws() {
        when(userRepository.findByIdIncludingDeleted("user-2")).thenReturn(Optional.of(deletedUser));
        assertThatThrownBy(() -> userService.toggleUserStatus("user-2", false))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already deactivated");
    }

    @Test
    void toggleUserStatus_userNotFound_throws() {
        when(userRepository.findByIdIncludingDeleted("missing")).thenReturn(Optional.empty());
        when(userRepository.findById("missing")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> userService.toggleUserStatus("missing", true))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void hardDeleteUser_success() {
        when(userRepository.findByIdIncludingDeleted("user-1")).thenReturn(Optional.of(activeUser));
        doNothing().when(userRepository).hardDeleteById("user-1");

        userService.hardDeleteUser("user-1");

        verify(userRepository).hardDeleteById("user-1");
    }

    @Test
    void hardDeleteUser_notFound_throws() {
        when(userRepository.findByIdIncludingDeleted("missing")).thenReturn(Optional.empty());
        when(userRepository.findById("missing")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> userService.hardDeleteUser("missing"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void hardDeleteUser_hardDeleteFails_fallback() {
        when(userRepository.findByIdIncludingDeleted("user-1")).thenReturn(Optional.of(activeUser));
        doThrow(new RuntimeException("db fail")).when(userRepository).hardDeleteById("user-1");
        doNothing().when(userRepository).deleteById("user-1");

        userService.hardDeleteUser("user-1");

        verify(userRepository).deleteById("user-1");
    }
}
