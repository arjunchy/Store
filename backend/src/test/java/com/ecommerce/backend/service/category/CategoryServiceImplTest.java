package com.ecommerce.backend.service.category;

import com.ecommerce.backend.dto.request.CategoryRequest;
import com.ecommerce.backend.dto.response.CategoryResponse;
import com.ecommerce.backend.entity.Category;
import com.ecommerce.backend.mapper.CategoryMapper;
import com.ecommerce.backend.repository.CategoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CategoryServiceImplTest {

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private CategoryMapper categoryMapper;

    @InjectMocks
    private CategoryServiceImpl categoryService;

    private Category parentCategory;
    private Category childCategory;
    private CategoryRequest createTopLevelRequest;
    private CategoryRequest createSubRequest;
    private CategoryResponse response;

    @BeforeEach
    void setUp() {
        parentCategory = Category.builder()
                .id("parent-1")
                .name("Electronics")
                .createdAt(LocalDateTime.now())
                .build();

        childCategory = Category.builder()
                .id("child-1")
                .name("Laptops")
                .parent(parentCategory)
                .createdAt(LocalDateTime.now())
                .build();

        createTopLevelRequest = new CategoryRequest("Electronics", null);
        createSubRequest = new CategoryRequest("Laptops", "parent-1");
        response = new CategoryResponse("cat-1", "Electronics", null, LocalDateTime.now());
    }

    // --- create ---

    @Test
    void create_topLevel_success() {
        Category mapped = Category.builder().name("Electronics").build();
        Category saved = Category.builder().id("cat-1").name("Electronics").createdAt(LocalDateTime.now()).build();
        CategoryResponse expected = new CategoryResponse("cat-1", "Electronics", null, LocalDateTime.now());

        when(categoryMapper.toEntity(createTopLevelRequest)).thenReturn(mapped);
        when(categoryRepository.save(any(Category.class))).thenReturn(saved);
        when(categoryMapper.toResponse(saved)).thenReturn(expected);

        CategoryResponse result = categoryService.create(createTopLevelRequest);

        assertThat(result).isEqualTo(expected);
        verify(categoryMapper).toEntity(createTopLevelRequest);
        verify(categoryRepository).save(argThat(c -> c.getParent() == null && c.getName().equals("Electronics")));
        verify(categoryRepository, never()).findById(anyString());
    }

    @Test
    void create_withBlankParentId_treatedAsTopLevel() {
        CategoryRequest req = new CategoryRequest("Books", "   ");
        Category mapped = Category.builder().name("Books").build();
        Category saved = Category.builder().id("cat-2").name("Books").build();
        CategoryResponse expected = new CategoryResponse("cat-2", "Books", null, LocalDateTime.now());

        when(categoryMapper.toEntity(req)).thenReturn(mapped);
        when(categoryRepository.save(any(Category.class))).thenReturn(saved);
        when(categoryMapper.toResponse(saved)).thenReturn(expected);

        CategoryResponse result = categoryService.create(req);

        assertThat(result).isEqualTo(expected);
        verify(categoryRepository, never()).findById(anyString());
        verify(categoryRepository).save(argThat(c -> c.getParent() == null));
    }

    @Test
    void create_withParent_success() {
        Category mapped = Category.builder().name("Laptops").build();
        Category saved = Category.builder().id("child-1").name("Laptops").parent(parentCategory).build();
        CategoryResponse expected = new CategoryResponse("child-1", "Laptops", "parent-1", LocalDateTime.now());

        when(categoryMapper.toEntity(createSubRequest)).thenReturn(mapped);
        when(categoryRepository.findById("parent-1")).thenReturn(Optional.of(parentCategory));
        when(categoryRepository.save(any(Category.class))).thenReturn(saved);
        when(categoryMapper.toResponse(saved)).thenReturn(expected);

        CategoryResponse result = categoryService.create(createSubRequest);

        assertThat(result.parentId()).isEqualTo("parent-1");
        verify(categoryRepository).findById("parent-1");
        verify(categoryRepository).save(argThat(c -> c.getParent() != null && c.getParent().getId().equals("parent-1")));
    }

    @Test
    void create_withParent_notFound_throws() {
        Category mapped = Category.builder().name("Laptops").build();
        when(categoryMapper.toEntity(createSubRequest)).thenReturn(mapped);
        when(categoryRepository.findById("parent-1")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> categoryService.create(createSubRequest))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Parent category not found");

        verify(categoryRepository, never()).save(any());
    }

    @Test
    void create_trimsParentIdCheck_blankVsNull() {
        CategoryRequest reqNullParent = new CategoryRequest("Toys", null);
        Category mapped = Category.builder().name("Toys").build();
        Category saved = Category.builder().id("id-toys").name("Toys").build();
        when(categoryMapper.toEntity(reqNullParent)).thenReturn(mapped);
        when(categoryRepository.save(any())).thenReturn(saved);
        when(categoryMapper.toResponse(saved)).thenReturn(new CategoryResponse("id-toys", "Toys", null, LocalDateTime.now()));

        CategoryResponse result = categoryService.create(reqNullParent);

        assertThat(result.name()).isEqualTo("Toys");
        verify(categoryRepository).save(argThat(c -> c.getParent() == null));
    }

    // --- getById ---

    @Test
    void getById_success() {
        when(categoryRepository.findById("cat-1")).thenReturn(Optional.of(parentCategory));
        CategoryResponse expected = new CategoryResponse(parentCategory.getId(), parentCategory.getName(), null, parentCategory.getCreatedAt());
        when(categoryMapper.toResponse(parentCategory)).thenReturn(expected);

        CategoryResponse result = categoryService.getById("cat-1");

        assertThat(result.id()).isEqualTo("parent-1");
        verify(categoryRepository).findById("cat-1");
    }

    @Test
    void getById_notFound_throws() {
        when(categoryRepository.findById("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> categoryService.getById("missing"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Category not found");
    }

    // --- getAll ---

    @Test
    void getAll_onlyTopLevel_true_callsFindByParentIdIsNull() {
        when(categoryRepository.findByParentIdIsNull()).thenReturn(List.of(parentCategory));
        CategoryResponse resp = new CategoryResponse(parentCategory.getId(), parentCategory.getName(), null, parentCategory.getCreatedAt());
        when(categoryMapper.toResponse(parentCategory)).thenReturn(resp);

        List<CategoryResponse> result = categoryService.getAll(true);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).id()).isEqualTo("parent-1");
        verify(categoryRepository).findByParentIdIsNull();
        verify(categoryRepository, never()).findAll();
    }

    @Test
    void getAll_onlyTopLevel_false_callsFindAll() {
        when(categoryRepository.findAll()).thenReturn(List.of(parentCategory, childCategory));
        when(categoryMapper.toResponse(parentCategory)).thenReturn(new CategoryResponse(parentCategory.getId(), parentCategory.getName(), null, parentCategory.getCreatedAt()));
        when(categoryMapper.toResponse(childCategory)).thenReturn(new CategoryResponse(childCategory.getId(), childCategory.getName(), parentCategory.getId(), childCategory.getCreatedAt()));

        List<CategoryResponse> result = categoryService.getAll(false);

        assertThat(result).hasSize(2);
        verify(categoryRepository).findAll();
        verify(categoryRepository, never()).findByParentIdIsNull();
    }

    @Test
    void getAll_empty_returnsEmptyList() {
        when(categoryRepository.findAll()).thenReturn(List.of());

        List<CategoryResponse> result = categoryService.getAll(false);

        assertThat(result).isEmpty();
    }

    @Test
    void getAll_topLevel_empty_returnsEmpty() {
        when(categoryRepository.findByParentIdIsNull()).thenReturn(List.of());

        List<CategoryResponse> result = categoryService.getAll(true);

        assertThat(result).isEmpty();
    }

    // --- update ---

    @Test
    void update_success_withoutParentChange_movesToTopLevel() {
        Category existing = Category.builder().id("cat-1").name("Old").parent(parentCategory).build();
        CategoryRequest req = new CategoryRequest("New Name", null);
        Category saved = Category.builder().id("cat-1").name("New Name").parent(null).build();
        CategoryResponse expected = new CategoryResponse("cat-1", "New Name", null, LocalDateTime.now());

        when(categoryRepository.findById("cat-1")).thenReturn(Optional.of(existing));
        when(categoryRepository.save(any(Category.class))).thenReturn(saved);
        when(categoryMapper.toResponse(saved)).thenReturn(expected);

        CategoryResponse result = categoryService.update("cat-1", req);

        assertThat(result.name()).isEqualTo("New Name");
        assertThat(result.parentId()).isNull();
        verify(categoryMapper).updateEntity(existing, req);
        verify(categoryRepository).save(argThat(c -> c.getParent() == null));
    }

    @Test
    void update_success_withBlankParentId_movesToTopLevel() {
        Category existing = Category.builder().id("cat-1").name("Old").parent(parentCategory).build();
        CategoryRequest req = new CategoryRequest("New", "  ");
        Category saved = Category.builder().id("cat-1").name("New").parent(null).build();
        when(categoryRepository.findById("cat-1")).thenReturn(Optional.of(existing));
        when(categoryRepository.save(any())).thenReturn(saved);
        when(categoryMapper.toResponse(saved)).thenReturn(new CategoryResponse("cat-1", "New", null, LocalDateTime.now()));

        CategoryResponse result = categoryService.update("cat-1", req);

        assertThat(result.parentId()).isNull();
        verify(categoryRepository).save(argThat(c -> c.getParent() == null));
    }

    @Test
    void update_success_withNewParent() {
        Category existing = Category.builder().id("child-1").name("Old").parent(null).build();
        Category newParent = Category.builder().id("parent-2").name("New Parent").build();
        CategoryRequest req = new CategoryRequest("Updated", "parent-2");
        Category saved = Category.builder().id("child-1").name("Updated").parent(newParent).build();
        CategoryResponse expected = new CategoryResponse("child-1", "Updated", "parent-2", LocalDateTime.now());

        when(categoryRepository.findById("child-1")).thenReturn(Optional.of(existing));
        when(categoryRepository.findById("parent-2")).thenReturn(Optional.of(newParent));
        when(categoryRepository.save(any())).thenReturn(saved);
        when(categoryMapper.toResponse(saved)).thenReturn(expected);

        CategoryResponse result = categoryService.update("child-1", req);

        assertThat(result.parentId()).isEqualTo("parent-2");
        verify(categoryRepository).findById("parent-2");
        verify(categoryRepository).save(argThat(c -> c.getParent() != null && c.getParent().getId().equals("parent-2")));
    }

    @Test
    void update_notFound_throws() {
        when(categoryRepository.findById("missing")).thenReturn(Optional.empty());
        CategoryRequest req = new CategoryRequest("New", null);

        assertThatThrownBy(() -> categoryService.update("missing", req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Category not found");
    }

    @Test
    void update_parentNotFound_throws() {
        Category existing = Category.builder().id("cat-1").name("Old").build();
        when(categoryRepository.findById("cat-1")).thenReturn(Optional.of(existing));
        CategoryRequest req = new CategoryRequest("New", "nonexistent");
        when(categoryRepository.findById("nonexistent")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> categoryService.update("cat-1", req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Parent category not found");

        verify(categoryRepository, never()).save(any());
    }

    @Test
    void update_selfParent_throws() {
        Category existing = Category.builder().id("cat-1").name("Old").build();
        when(categoryRepository.findById("cat-1")).thenReturn(Optional.of(existing));
        CategoryRequest req = new CategoryRequest("New", "cat-1");

        assertThatThrownBy(() -> categoryService.update("cat-1", req))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Category cannot be its own parent");

        verify(categoryRepository, never()).save(any());
    }

    @Test
    void update_callsMapperUpdateEntity() {
        Category existing = Category.builder().id("cat-1").name("Old").build();
        CategoryRequest req = new CategoryRequest("New", null);
        Category saved = Category.builder().id("cat-1").name("New").build();
        when(categoryRepository.findById("cat-1")).thenReturn(Optional.of(existing));
        when(categoryRepository.save(any())).thenReturn(saved);
        when(categoryMapper.toResponse(saved)).thenReturn(new CategoryResponse("cat-1", "New", null, LocalDateTime.now()));

        categoryService.update("cat-1", req);

        verify(categoryMapper).updateEntity(existing, req);
    }

    // --- delete ---

    @Test
    void delete_success() {
        when(categoryRepository.existsById("cat-1")).thenReturn(true);
        doNothing().when(categoryRepository).deleteById("cat-1");

        categoryService.delete("cat-1");

        verify(categoryRepository).existsById("cat-1");
        verify(categoryRepository).deleteById("cat-1");
    }

    @Test
    void delete_notFound_throws() {
        when(categoryRepository.existsById("missing")).thenReturn(false);

        assertThatThrownBy(() -> categoryService.delete("missing"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Category not found");

        verify(categoryRepository, never()).deleteById(anyString());
    }

    @Test
    void delete_existing_thenVerifyHardDelete() {
        when(categoryRepository.existsById("parent-1")).thenReturn(true);
        doNothing().when(categoryRepository).deleteById("parent-1");

        categoryService.delete("parent-1");

        verify(categoryRepository).deleteById("parent-1");
    }
}
