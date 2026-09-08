package com.ecommerce.backend.controller;

import com.ecommerce.backend.config.CustomUserDetails;
import com.ecommerce.backend.dto.request.AddressRequest;
import com.ecommerce.backend.dto.response.AddressResponse;
import com.ecommerce.backend.service.address.AddressService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AddressControllerTest {

    @Mock
    private AddressService addressService;

    @InjectMocks
    private AddressController addressController;

    @Mock
    private CustomUserDetails userDetails;

    private AddressResponse sampleResponse;
    private AddressRequest sampleRequest;

    @BeforeEach
    void setUp() {
        sampleResponse = new AddressResponse(
                "addr-1", "Home", "123 Main St", "Springfield", "IL",
                "62701", "US", true, LocalDateTime.now()
        );
        sampleRequest = new AddressRequest("Home", "123 Main St", "Springfield", "IL", "62701", "US", true);
    }

    @Test
    void create_success_returns201() {
        when(userDetails.getUserId()).thenReturn("user-1");
        when(addressService.create(any(AddressRequest.class), eq("user-1"))).thenReturn(sampleResponse);

        ResponseEntity<AddressResponse> response = addressController.create(sampleRequest, userDetails);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody().id()).isEqualTo("addr-1");
        verify(addressService).create(sampleRequest, "user-1");
    }

    @Test
    void create_propagatesUserNotFound() {
        when(userDetails.getUserId()).thenReturn("user-1");
        when(addressService.create(any(), eq("user-1"))).thenThrow(new IllegalArgumentException("User not found"));

        assertThatThrownBy(() -> addressController.create(sampleRequest, userDetails))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("User not found");
    }

    @Test
    void getAll_success_returns200() {
        when(userDetails.getUserId()).thenReturn("user-1");
        when(addressService.getAll("user-1")).thenReturn(List.of(sampleResponse));

        ResponseEntity<List<AddressResponse>> response = addressController.getAll(userDetails);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(1);
        assertThat(response.getBody().get(0).id()).isEqualTo("addr-1");
    }

    @Test
    void getAll_empty_returnsEmptyList() {
        when(userDetails.getUserId()).thenReturn("user-1");
        when(addressService.getAll("user-1")).thenReturn(List.of());

        ResponseEntity<List<AddressResponse>> response = addressController.getAll(userDetails);

        assertThat(response.getBody()).isEmpty();
    }

    @Test
    void getById_success_returns200() {
        when(userDetails.getUserId()).thenReturn("user-1");
        when(addressService.getById("addr-1", "user-1")).thenReturn(sampleResponse);

        ResponseEntity<AddressResponse> response = addressController.getById("addr-1", userDetails);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().id()).isEqualTo("addr-1");
    }

    @Test
    void getById_notFound_propagates() {
        when(userDetails.getUserId()).thenReturn("user-1");
        when(addressService.getById("missing", "user-1")).thenThrow(new IllegalArgumentException("Address not found"));

        assertThatThrownBy(() -> addressController.getById("missing", userDetails))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void update_success_returns200() {
        AddressRequest updateReq = new AddressRequest("Work", "456 Oak Ave", "Chicago", "IL", "60601", "US", false);
        Map<String, Object> updateBody = new HashMap<>();
        updateBody.put("label", "Work");
        updateBody.put("street", "456 Oak Ave");
        AddressResponse updated = new AddressResponse("addr-1", "Work", "456 Oak Ave", "Chicago", "IL", "60601", "US", false, LocalDateTime.now());
        when(userDetails.getUserId()).thenReturn("user-1");
        when(addressService.updatePartial(eq("addr-1"), any(), eq("user-1"))).thenReturn(updated);

        ResponseEntity<AddressResponse> response = addressController.update("addr-1", updateBody, userDetails);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().label()).isEqualTo("Work");
    }

    @Test
    void update_notFound_propagates() {
        Map<String, Object> updateBody = new HashMap<>();
        updateBody.put("street", "456 Oak Ave");
        when(userDetails.getUserId()).thenReturn("user-1");
        when(addressService.updatePartial(eq("missing"), any(), eq("user-1"))).thenThrow(new IllegalArgumentException("Address not found"));

        assertThatThrownBy(() -> addressController.update("missing", updateBody, userDetails))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void delete_success_returns204() {
        when(userDetails.getUserId()).thenReturn("user-1");
        doNothing().when(addressService).delete("addr-1", "user-1");

        ResponseEntity<Void> response = addressController.delete("addr-1", userDetails);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(addressService).delete("addr-1", "user-1");
    }

    @Test
    void delete_notFound_propagates() {
        when(userDetails.getUserId()).thenReturn("user-1");
        doThrow(new IllegalArgumentException("Address not found")).when(addressService).delete("missing", "user-1");

        assertThatThrownBy(() -> addressController.delete("missing", userDetails))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
