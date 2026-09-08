package com.ecommerce.backend.controller;

import com.ecommerce.backend.dto.request.CategoryRequest;
import com.ecommerce.backend.dto.response.CategoryResponse;
import com.ecommerce.backend.service.category.CategoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CategoryControllerTest {

    @Mock
    private CategoryService categoryService;

    @InjectMocks
    private CategoryController categoryController;

    private CategoryResponse sampleResponse;
    private CategoryRequest sampleRequest;

    @BeforeEach
    void setUp() {
        sampleResponse = new CategoryResponse("cat-1", "Electronics", null, LocalDateTime.now());
        sampleRequest = new CategoryRequest("Electronics", null);
    }

    @Test
    void create_success_returns201() {
        when(categoryService.create(any(CategoryRequest.class))).thenReturn(sampleResponse);

        ResponseEntity<CategoryResponse> response = categoryController.create(sampleRequest);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isEqualTo(sampleResponse);
        verify(categoryService).create(sampleRequest);
    }

    @Test
    void create_withParentId_delegatesToService() {
        CategoryRequest subRequest = new CategoryRequest("Laptops", "parent-1");
        CategoryResponse subResponse = new CategoryResponse("child-1", "Laptops", "parent-1", LocalDateTime.now());
        when(categoryService.create(subRequest)).thenReturn(subResponse);

        ResponseEntity<CategoryResponse> response = categoryController.create(subRequest);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody().parentId()).isEqualTo("parent-1");
    }

    @Test
    void create_propagatesParentNotFound() {
        CategoryRequest req = new CategoryRequest("Laptops", "nonexistent");
        when(categoryService.create(any())).thenThrow(new IllegalArgumentException("Parent category not found"));

        assertThatThrownBy(() -> categoryController.create(req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Parent category not found");
    }

    @Test
    void getById_success_returns200() {
        when(categoryService.getById("cat-1")).thenReturn(sampleResponse);

        ResponseEntity<CategoryResponse> response = categoryController.getById("cat-1");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().id()).isEqualTo("cat-1");
        verify(categoryService).getById("cat-1");
    }

    @Test
    void getById_notFound_propagates() {
        when(categoryService.getById("missing")).thenThrow(new IllegalArgumentException("Category not found"));

        assertThatThrownBy(() -> categoryController.getById("missing"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void getAll_topLevelOnly_true() {
        when(categoryService.getAll(true)).thenReturn(List.of(sampleResponse));

        ResponseEntity<List<CategoryResponse>> response = categoryController.getAll(true);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(1);
        verify(categoryService).getAll(true);
    }

    @Test
    void getAll_topLevelOnly_false_returnsAll() {
        CategoryResponse child = new CategoryResponse("child-1", "Laptops", "cat-1", LocalDateTime.now());
        when(categoryService.getAll(false)).thenReturn(List.of(sampleResponse, child));

        ResponseEntity<List<CategoryResponse>> response = categoryController.getAll(false);

        assertThat(response.getBody()).hasSize(2);
        verify(categoryService).getAll(false);
    }

    @Test
    void getAll_default_returnsAll() {
        when(categoryService.getAll(false)).thenReturn(List.of());

        ResponseEntity<List<CategoryResponse>> response = categoryController.getAll(false);

        assertThat(response.getBody()).isEmpty();
    }

    @Test
    void update_success_returns200() {
        CategoryRequest updateReq = new CategoryRequest("Updated", null);
        CategoryResponse updated = new CategoryResponse("cat-1", "Updated", null, LocalDateTime.now());
        when(categoryService.update(eq("cat-1"), any(CategoryRequest.class))).thenReturn(updated);

        ResponseEntity<CategoryResponse> response = categoryController.update("cat-1", updateReq);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().name()).isEqualTo("Updated");
        verify(categoryService).update("cat-1", updateReq);
    }

    @Test
    void update_propagatesParentNotFound() {
        CategoryRequest req = new CategoryRequest("New", "bad-id");
        when(categoryService.update(anyString(), any())).thenThrow(new IllegalArgumentException("Parent category not found"));

        assertThatThrownBy(() -> categoryController.update("cat-1", req))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void update_selfParent_propagates() {
        CategoryRequest req = new CategoryRequest("New", "cat-1");
        when(categoryService.update(eq("cat-1"), any())).thenThrow(new IllegalArgumentException("Category cannot be its own parent"));

        assertThatThrownBy(() -> categoryController.update("cat-1", req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("own parent");
    }

    @Test
    void delete_success_returns204() {
        doNothing().when(categoryService).delete("cat-1");

        ResponseEntity<Void> response = categoryController.delete("cat-1");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(response.getBody()).isNull();
        verify(categoryService).delete("cat-1");
    }

    @Test
    void delete_notFound_propagates() {
        doThrow(new IllegalArgumentException("Category not found")).when(categoryService).delete("missing");

        assertThatThrownBy(() -> categoryController.delete("missing"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void delete_delegatesCorrectId() {
        doNothing().when(categoryService).delete("parent-1");

        categoryController.delete("parent-1");

        verify(categoryService).delete("parent-1");
    }
}
