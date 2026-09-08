package com.ecommerce.backend.service.category;

import com.ecommerce.backend.dto.request.CategoryRequest;
import com.ecommerce.backend.dto.response.CategoryResponse;

import java.util.List;

public interface CategoryService {

    CategoryResponse create(CategoryRequest request);

    CategoryResponse getById(String id);

    List<CategoryResponse> getAll(boolean onlyTopLevel);

    CategoryResponse update(String id, CategoryRequest request);

    void delete(String id);
}
