package com.ecommerce.backend.service.address;

import com.ecommerce.backend.dto.request.AddressRequest;
import com.ecommerce.backend.dto.response.AddressResponse;
import com.ecommerce.backend.entity.Address;
import com.ecommerce.backend.entity.User;
import com.ecommerce.backend.mapper.AddressMapper;
import com.ecommerce.backend.repository.AddressRepository;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AddressServiceTest {

    @Mock
    private AddressRepository addressRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AddressMapper addressMapper;

    @InjectMocks
    private AddressService addressService;

    private User user;
    private Address address;
    private AddressRequest createRequest;

    @BeforeEach
    void setUp() {
        user = User.builder().userId("user-1").email("test@test.com").build();
        address = Address.builder()
                .id("addr-1")
                .user(user)
                .label("Home")
                .street("123 Main St")
                .city("Springfield")
                .state("IL")
                .postalCode("62701")
                .country("US")
                .isDefault(true)
                .createdAt(LocalDateTime.now())
                .build();
        createRequest = new AddressRequest("Home", "123 Main St", "Springfield", "IL", "62701", "US", true);
    }

    @Test
    void create_success() {
        when(userRepository.findById("user-1")).thenReturn(Optional.of(user));
        when(addressRepository.save(any(Address.class))).thenReturn(address);
        when(addressMapper.toResponse(any(Address.class))).thenReturn(
                new AddressResponse("addr-1", "Home", "123 Main St", "Springfield", "IL", "62701", "US", true, LocalDateTime.now())
        );

        AddressResponse response = addressService.create(createRequest, "user-1");

        assertThat(response.id()).isEqualTo("addr-1");
        verify(addressRepository).unsetOtherDefaults("user-1");
        verify(addressRepository).save(any(Address.class));
    }

    @Test
    void create_isDefaultFalse_skipsUnset() {
        AddressRequest req = new AddressRequest("Work", "456 Oak Ave", "Chicago", "IL", "60601", "US", false);
        Address saved = Address.builder().id("addr-2").user(user).label("Work").isDefault(false).createdAt(LocalDateTime.now()).build();
        when(userRepository.findById("user-1")).thenReturn(Optional.of(user));
        when(addressRepository.save(any())).thenReturn(saved);
        when(addressMapper.toResponse(any())).thenReturn(
                new AddressResponse("addr-2", "Work", "456 Oak Ave", "Chicago", "IL", "60601", "US", false, LocalDateTime.now())
        );

        addressService.create(req, "user-1");

        verify(addressRepository, never()).unsetOtherDefaults(anyString());
    }

    @Test
    void create_userNotFound_throws() {
        when(userRepository.findById("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> addressService.create(createRequest, "missing"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("User not found");
    }

    @Test
    void getAll_success() {
        when(addressRepository.findByUserId("user-1")).thenReturn(List.of(address));
        when(addressMapper.toResponse(address)).thenReturn(
                new AddressResponse("addr-1", "Home", "123 Main St", "Springfield", "IL", "62701", "US", true, LocalDateTime.now())
        );

        List<AddressResponse> result = addressService.getAll("user-1");

        assertThat(result).hasSize(1);
        verify(addressRepository).findByUserId("user-1");
    }

    @Test
    void getAll_empty() {
        when(addressRepository.findByUserId("user-1")).thenReturn(List.of());

        List<AddressResponse> result = addressService.getAll("user-1");

        assertThat(result).isEmpty();
    }

    @Test
    void getById_success() {
        when(addressRepository.findById("addr-1")).thenReturn(Optional.of(address));
        when(addressMapper.toResponse(address)).thenReturn(
                new AddressResponse("addr-1", "Home", "123 Main St", "Springfield", "IL", "62701", "US", true, LocalDateTime.now())
        );

        AddressResponse result = addressService.getById("addr-1", "user-1");

        assertThat(result.id()).isEqualTo("addr-1");
    }

    @Test
    void getById_notFound_throws() {
        when(addressRepository.findById("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> addressService.getById("missing", "user-1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Address not found");
    }

    @Test
    void getById_wrongOwner_throws() {
        User otherUser = User.builder().userId("user-2").build();
        Address otherAddress = Address.builder().id("addr-2").user(otherUser).build();
        when(addressRepository.findById("addr-2")).thenReturn(Optional.of(otherAddress));

        assertThatThrownBy(() -> addressService.getById("addr-2", "user-1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Address not found");
    }

    @Test
    void update_success() {
        AddressRequest updateReq = new AddressRequest("Work", "456 Oak Ave", "Chicago", null, "60601", "US", true);
        when(addressRepository.findById("addr-1")).thenReturn(Optional.of(address));
        when(addressRepository.save(any())).thenReturn(address);
        when(addressMapper.toResponse(any())).thenReturn(
                new AddressResponse("addr-1", "Work", "456 Oak Ave", "Chicago", null, "60601", "US", true, LocalDateTime.now())
        );

        AddressResponse result = addressService.update("addr-1", updateReq, "user-1");

        assertThat(result.label()).isEqualTo("Work");
    }

    @Test
    void update_notFound_throws() {
        when(addressRepository.findById("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> addressService.update("missing", createRequest, "user-1"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void update_wrongOwner_throws() {
        User otherUser = User.builder().userId("user-2").build();
        Address otherAddress = Address.builder().id("addr-2").user(otherUser).build();
        when(addressRepository.findById("addr-2")).thenReturn(Optional.of(otherAddress));

        assertThatThrownBy(() -> addressService.update("addr-2", createRequest, "user-1"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void delete_success() {
        when(addressRepository.findById("addr-1")).thenReturn(Optional.of(address));
        doNothing().when(addressRepository).delete(address);

        addressService.delete("addr-1", "user-1");

        verify(addressRepository).delete(address);
    }

    @Test
    void delete_notFound_throws() {
        when(addressRepository.findById("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> addressService.delete("missing", "user-1"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void delete_wrongOwner_throws() {
        User otherUser = User.builder().userId("user-2").build();
        Address otherAddress = Address.builder().id("addr-2").user(otherUser).build();
        when(addressRepository.findById("addr-2")).thenReturn(Optional.of(otherAddress));

        assertThatThrownBy(() -> addressService.delete("addr-2", "user-1"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
