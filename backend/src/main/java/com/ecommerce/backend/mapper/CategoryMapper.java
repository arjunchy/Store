package com.ecommerce.backend.mapper;

import com.ecommerce.backend.dto.request.CategoryRequest;
import com.ecommerce.backend.dto.response.CategoryResponse;
import com.ecommerce.backend.entity.Category;
import org.springframework.stereotype.Component;

@Component
public class CategoryMapper {

    public Category toEntity(CategoryRequest request) {
        if (request == null) {
            return null;
        }
        Category category = new Category();
        category.setName(request.name());
        return category;
    }

    public CategoryResponse toResponse(Category category) {
        if (category == null) {
            return null;
        }
        return new CategoryResponse(
                category.getId(),
                category.getName(),
                category.getParent() != null ? category.getParent().getId() : null,
                category.getCreatedAt()
        );
    }

    public void updateEntity(Category category, CategoryRequest request) {
        if (category == null || request == null) {
            return;
        }
        category.setName(request.name());
    }
}
