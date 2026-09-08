package com.ecommerce.backend.service.user;

import com.ecommerce.backend.dto.request.UserRequest;
import com.ecommerce.backend.dto.response.UserResponse;
import com.ecommerce.backend.dto.request.UserUpdateRequest;
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
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private UserServiceImpl userService;

    private User activeUser;
    private User softDeletedUser;
    private UserRequest registerRequest;
    private UserUpdateRequest updateRequest;
    private UserResponse userResponse;

    @BeforeEach
    void setUp() {
        activeUser = User.builder()
                .userId("uuid-1")
                .username("john")
                .email("john@example.com")
                .passwordHash("hashed")
                .userRole(UserRole.USER)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .deletedAt(null)
                .build();

        softDeletedUser = User.builder()
                .userId("uuid-2")
                .username("deleted")
                .email("deleted@example.com")
                .passwordHash("hashed")
                .userRole(UserRole.USER)
                .deletedAt(LocalDateTime.now().minusDays(1))
                .createdAt(LocalDateTime.now().minusDays(2))
                .build();

        registerRequest = new UserRequest("john", "john@example.com", "password123");
        updateRequest = new UserUpdateRequest("john-updated", "john@example.com", null);
        userResponse = new UserResponse("uuid-1", "john", "john@example.com", UserRole.USER, LocalDateTime.now());
    }

    // --- register ---

    @Test
    void register_success_createsNewUser() {
        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.empty());
        User mapped = User.builder().username("john").email("john@example.com").build();
        when(userMapper.toEntity(registerRequest)).thenReturn(mapped);
        when(passwordEncoder.encode("password123")).thenReturn("encoded123");
        User saved = User.builder().userId("uuid-1").username("john").email("john@example.com").passwordHash("encoded123").userRole(UserRole.USER).build();
        when(userRepository.save(any(User.class))).thenReturn(saved);
        when(userMapper.toResponse(saved)).thenReturn(userResponse);

        UserResponse result = userService.register(registerRequest);

        assertThat(result).isEqualTo(userResponse);
        verify(userRepository).save(any(User.class));
        verify(passwordEncoder).encode("password123");
    }

    @Test
    void register_throwsWhenActiveEmailExists() {
        when(userRepository.findByEmail("john@example.com")).thenReturn(Optional.of(activeUser));

        assertThatThrownBy(() -> userService.register(registerRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Email already exists");

        verify(userRepository, never()).save(any());
    }

    @Test
    void register_reactivatesSoftDeletedUser() {
        when(userRepository.findByEmail("deleted@example.com")).thenReturn(Optional.of(softDeletedUser));
        UserRequest req = new UserRequest("newname", "deleted@example.com", "newpass123");
        when(passwordEncoder.encode("newpass123")).thenReturn("encodedNew");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        User savedReactivated = User.builder().userId("uuid-2").username("newname").email("deleted@example.com").passwordHash("encodedNew").userRole(UserRole.USER).deletedAt(null).build();
        when(userMapper.toResponse(any(User.class))).thenReturn(new UserResponse("uuid-2", "newname", "deleted@example.com", UserRole.USER, LocalDateTime.now()));

        UserResponse result = userService.register(req);

        assertThat(result.email()).isEqualTo("deleted@example.com");
        verify(userRepository).save(argThat(u -> u.getDeletedAt() == null && u.getUsername().equals("newname")));
    }

    @Test
    void register_setsRoleToUSER_evenIfMapperSetsOther() {
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());
        User mapped = new User();
        mapped.setUsername("john");
        mapped.setEmail("john@example.com");
        mapped.setUserRole(UserRole.ADMIN);
        when(userMapper.toEntity(registerRequest)).thenReturn(mapped);
        when(passwordEncoder.encode(anyString())).thenReturn("enc");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(userMapper.toResponse(any(User.class))).thenReturn(userResponse);

        userService.register(registerRequest);

        verify(userRepository).save(argThat(u -> u.getUserRole() == UserRole.USER));
    }

    // --- getCurrentUser ---

    @Test
    void getCurrentUser_success() {
        when(userRepository.findActiveByEmail("john@example.com")).thenReturn(Optional.of(activeUser));
        when(userMapper.toResponse(activeUser)).thenReturn(userResponse);

        UserResponse result = userService.getCurrentUser("john@example.com");

        assertThat(result).isEqualTo(userResponse);
    }

    @Test
    void getCurrentUser_notFound_throws() {
        when(userRepository.findActiveByEmail("missing@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getCurrentUser("missing@example.com"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("User not found");
    }

    @Test
    void getCurrentUser_softDeleted_notFound() {
        when(userRepository.findActiveByEmail("deleted@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getCurrentUser("deleted@example.com"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // --- getAll ---

    @Test
    void getAll_returnsOnlyActiveUsers() {
        User u2 = User.builder().userId("2").username("jane").email("jane@example.com").userRole(UserRole.USER).build();
        when(userRepository.findAllActive()).thenReturn(List.of(activeUser, u2));
        when(userMapper.toResponse(activeUser)).thenReturn(userResponse);
        when(userMapper.toResponse(u2)).thenReturn(new UserResponse("2", "jane", "jane@example.com", UserRole.USER, LocalDateTime.now()));

        List<UserResponse> result = userService.getAll();

        assertThat(result).hasSize(2);
        verify(userRepository).findAllActive();
        verify(userRepository, never()).findAll();
    }

    @Test
    void getAll_empty_returnsEmptyList() {
        when(userRepository.findAllActive()).thenReturn(List.of());

        List<UserResponse> result = userService.getAll();

        assertThat(result).isEmpty();
    }

    // --- updateCurrentUser ---

    @Test
    void updateCurrentUser_success_noEmailChange_noPasswordChange() {
        when(userRepository.findActiveByEmail("john@example.com")).thenReturn(Optional.of(activeUser));
        UserUpdateRequest req = new UserUpdateRequest("john2", "john@example.com", null);
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(userMapper.toResponse(any(User.class))).thenReturn(new UserResponse("uuid-1", "john2", "john@example.com", UserRole.USER, LocalDateTime.now()));

        UserResponse result = userService.updateCurrentUser("john@example.com", req);

        assertThat(result.username()).isEqualTo("john2");
        verify(userRepository).save(argThat(u -> u.getUsername().equals("john2") && u.getPasswordHash().equals("hashed")));
        verify(passwordEncoder, never()).encode(anyString());
    }

    @Test
    void updateCurrentUser_success_emailChange() {
        when(userRepository.findActiveByEmail("john@example.com")).thenReturn(Optional.of(activeUser));
        when(userRepository.existsActiveByEmail("new@example.com")).thenReturn(false);
        UserUpdateRequest req = new UserUpdateRequest("john", "new@example.com", null);
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(userMapper.toResponse(any(User.class))).thenReturn(new UserResponse("uuid-1", "john", "new@example.com", UserRole.USER, LocalDateTime.now()));

        UserResponse result = userService.updateCurrentUser("john@example.com", req);

        assertThat(result.email()).isEqualTo("new@example.com");
    }

    @Test
    void updateCurrentUser_throwsWhenNewEmailAlreadyExists() {
        when(userRepository.findActiveByEmail("john@example.com")).thenReturn(Optional.of(activeUser));
        when(userRepository.existsActiveByEmail("taken@example.com")).thenReturn(true);
        UserUpdateRequest req = new UserUpdateRequest("john", "taken@example.com", null);

        assertThatThrownBy(() -> userService.updateCurrentUser("john@example.com", req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Email already exists");

        verify(userRepository, never()).save(any());
    }

    @Test
    void updateCurrentUser_success_passwordUpdate() {
        when(userRepository.findActiveByEmail("john@example.com")).thenReturn(Optional.of(activeUser));
        UserUpdateRequest req = new UserUpdateRequest("john", "john@example.com", "newPassword123");
        when(passwordEncoder.encode("newPassword123")).thenReturn("encodedNew");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(userMapper.toResponse(any(User.class))).thenReturn(userResponse);

        userService.updateCurrentUser("john@example.com", req);

        verify(passwordEncoder).encode("newPassword123");
        verify(userRepository).save(argThat(u -> u.getPasswordHash().equals("encodedNew")));
    }

    @Test
    void updateCurrentUser_blankPassword_doesNotUpdate() {
        when(userRepository.findActiveByEmail("john@example.com")).thenReturn(Optional.of(activeUser));
        UserUpdateRequest req = new UserUpdateRequest("john", "john@example.com", "   ");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(userMapper.toResponse(any(User.class))).thenReturn(userResponse);

        userService.updateCurrentUser("john@example.com", req);

        verify(passwordEncoder, never()).encode(anyString());
        verify(userRepository).save(argThat(u -> u.getPasswordHash().equals("hashed")));
    }

    @Test
    void updateCurrentUser_userNotFound_throws() {
        when(userRepository.findActiveByEmail("missing@example.com")).thenReturn(Optional.empty());
        UserUpdateRequest req = new UserUpdateRequest("x", "missing@example.com", null);

        assertThatThrownBy(() -> userService.updateCurrentUser("missing@example.com", req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("User not found");
    }

    @Test
    void updateCurrentUser_emailUnchanged_doesNotCheckExists() {
        when(userRepository.findActiveByEmail("john@example.com")).thenReturn(Optional.of(activeUser));
        UserUpdateRequest req = new UserUpdateRequest("john2", "john@example.com", null);
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        when(userMapper.toResponse(any(User.class))).thenReturn(userResponse);

        userService.updateCurrentUser("john@example.com", req);

        verify(userRepository, never()).existsActiveByEmail(anyString());
    }

    // --- deleteCurrentUser ---

    @Test
    void deleteCurrentUser_success_softDeletes() {
        when(userRepository.findActiveByEmail("john@example.com")).thenReturn(Optional.of(activeUser));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        userService.deleteCurrentUser("john@example.com");

        verify(userRepository).save(argThat(u -> u.getDeletedAt() != null));
        verify(userRepository, never()).delete(any());
    }

    @Test
    void deleteCurrentUser_notFound_throws() {
        when(userRepository.findActiveByEmail("missing@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.deleteCurrentUser("missing@example.com"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("User not found");

        verify(userRepository, never()).save(any());
    }

    @Test
    void deleteCurrentUser_softDeleted_alreadyDeleted_throws() {
        when(userRepository.findActiveByEmail("deleted@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.deleteCurrentUser("deleted@example.com"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
