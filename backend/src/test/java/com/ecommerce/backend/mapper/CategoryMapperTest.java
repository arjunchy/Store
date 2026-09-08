package com.ecommerce.backend.mapper;

import com.ecommerce.backend.dto.request.CategoryRequest;
import com.ecommerce.backend.dto.response.CategoryResponse;
import com.ecommerce.backend.entity.Category;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class CategoryMapperTest {

    private final CategoryMapper mapper = new CategoryMapper();

    @Test
    void toEntity_mapsName() {
        CategoryRequest request = new CategoryRequest("Electronics", "parent-123");

        Category entity = mapper.toEntity(request);

        assertThat(entity.getName()).isEqualTo("Electronics");
        assertThat(entity.getId()).isNull();
        assertThat(entity.getParent()).isNull();
    }

    @Test
    void toEntity_handlesNullRequest() {
        Category entity = mapper.toEntity(null);

        assertThat(entity).isNull();
    }

    @Test
    void toEntity_handlesNullFields() {
        CategoryRequest request = new CategoryRequest(null, null);

        Category entity = mapper.toEntity(request);

        assertThat(entity.getName()).isNull();
    }

    @Test
    void toEntity_doesNotMapParentId() {
        CategoryRequest request = new CategoryRequest("Laptops", "parent-id");

        Category entity = mapper.toEntity(request);

        // parent should not be set by mapper, service handles it
        assertThat(entity.getParent()).isNull();
    }

    @Test
    void toResponse_mapsAllFields_withoutParent() {
        LocalDateTime now = LocalDateTime.now();
        Category category = Category.builder()
                .id("cat-1")
                .name("Electronics")
                .parent(null)
                .createdAt(now)
                .build();

        CategoryResponse response = mapper.toResponse(category);

        assertThat(response.id()).isEqualTo("cat-1");
        assertThat(response.name()).isEqualTo("Electronics");
        assertThat(response.parentId()).isNull();
        assertThat(response.createdAt()).isEqualTo(now);
    }

    @Test
    void toResponse_mapsParentId_whenParentExists() {
        Category parent = Category.builder().id("parent-1").name("Electronics").build();
        Category child = Category.builder()
                .id("child-1")
                .name("Laptops")
                .parent(parent)
                .createdAt(LocalDateTime.now())
                .build();

        CategoryResponse response = mapper.toResponse(child);

        assertThat(response.parentId()).isEqualTo("parent-1");
        assertThat(response.id()).isEqualTo("child-1");
    }

    @Test
    void toResponse_handlesNullCategory() {
        CategoryResponse response = mapper.toResponse(null);

        assertThat(response).isNull();
    }

    @Test
    void toResponse_mapsAllFields_directlyInMapper() {
        Category parent = Category.builder().id("p1").name("P").build();
        LocalDateTime now = LocalDateTime.now();
        Category category = Category.builder().id("c1").name("C").parent(parent).createdAt(now).build();

        CategoryResponse response = mapper.toResponse(category);

        assertThat(response.id()).isEqualTo("c1");
        assertThat(response.name()).isEqualTo("C");
        assertThat(response.parentId()).isEqualTo("p1");
        assertThat(response.createdAt()).isEqualTo(now);
    }

    @Test
    void updateEntity_updatesName() {
        Category category = Category.builder()
                .id("cat-1")
                .name("Old Name")
                .build();
        CategoryRequest request = new CategoryRequest("New Name", "ignored");

        mapper.updateEntity(category, request);

        assertThat(category.getName()).isEqualTo("New Name");
        // parent not changed by updateEntity
        assertThat(category.getParent()).isNull();
    }

    @Test
    void updateEntity_handlesNullInputs() {
        Category category = Category.builder().name("Old").build();
        mapper.updateEntity(null, new CategoryRequest("a", null));
        mapper.updateEntity(category, null);

        // should not throw and not change when null
        assertThat(category.getName()).isEqualTo("Old");
    }

    @Test
    void updateEntity_doesNotUpdateParent() {
        Category parent = Category.builder().id("parent-1").name("Parent").build();
        Category category = Category.builder().id("cat-1").name("Child").parent(parent).build();
        CategoryRequest request = new CategoryRequest("Updated", "new-parent-id");

        mapper.updateEntity(category, request);

        // parent should remain old parent, service handles parent change
        assertThat(category.getParent()).isEqualTo(parent);
        assertThat(category.getParent().getId()).isEqualTo("parent-1");
    }
}
