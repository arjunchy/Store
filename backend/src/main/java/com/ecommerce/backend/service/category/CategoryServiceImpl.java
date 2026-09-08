package com.ecommerce.backend.service.category;

import com.ecommerce.backend.dto.request.CategoryRequest;
import com.ecommerce.backend.dto.response.CategoryResponse;
import com.ecommerce.backend.entity.Category;
import com.ecommerce.backend.entity.Product;
import com.ecommerce.backend.mapper.CategoryMapper;
import com.ecommerce.backend.repository.CategoryRepository;
import com.ecommerce.backend.repository.ProductRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Slf4j
public class CategoryServiceImpl implements CategoryService {

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private CategoryMapper categoryMapper;

    @Override
    @Transactional
    public CategoryResponse create(CategoryRequest request) {
        log.info("Creating category with name: {} and parentId: {}", request.name(), request.parentId());
        Category category = categoryMapper.toEntity(request);

        if (request.parentId() != null && !request.parentId().isBlank()) {
            log.debug("Looking up parent category with id: {}", request.parentId());
            Category parentCategory = categoryRepository.findById(request.parentId())
                    .orElseThrow(() -> {
                        log.warn("Parent category not found with id: {}", request.parentId());
                        return new IllegalArgumentException("Parent category not found");
                    });
            category.setParent(parentCategory);
            log.debug("Set parent category {} for new category", parentCategory.getId());
        } else {
            category.setParent(null);
            log.debug("Creating top-level category (no parent)");
        }

        Category saved = categoryRepository.save(category);
        log.info("Successfully created category with id: {} and name: {}", saved.getId(), saved.getName());
        return categoryMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public CategoryResponse getById(String id) {
        log.debug("Fetching category by id: {}", id);
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Category not found with id: {}", id);
                    return new IllegalArgumentException("Category not found");
                });
        log.debug("Found category with id: {} and name: {}", category.getId(), category.getName());
        return categoryMapper.toResponse(category);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoryResponse> getAll(boolean onlyTopLevel) {
        log.debug("Fetching all categories, onlyTopLevel={}", onlyTopLevel);
        List<Category> categories;
        if (onlyTopLevel) {
            categories = categoryRepository.findByParentIdIsNull();
            log.info("Retrieved {} top-level categories", categories.size());
        } else {
            categories = categoryRepository.findAll();
            log.info("Retrieved {} total categories", categories.size());
        }
        return categories.stream()
                .map(categoryMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public CategoryResponse update(String id, CategoryRequest request) {
        log.info("Updating category with id: {} and new name: {}", id, request.name());
        Category existing = categoryRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Category not found for update with id: {}", id);
                    return new IllegalArgumentException("Category not found");
                });

        categoryMapper.updateEntity(existing, request);
        log.debug("Updated name to {} for category id: {}", request.name(), id);

        if (request.parentId() != null && !request.parentId().isBlank()) {
            if (request.parentId().equals(id)) {
                log.warn("Category cannot be its own parent: id={} parentId={}", id, request.parentId());
                throw new IllegalArgumentException("Category cannot be its own parent");
            }
            assertNoCycle(id, request.parentId());
            log.debug("Looking up new parent category with id: {}", request.parentId());
            Category parentCategory = categoryRepository.findById(request.parentId())
                    .orElseThrow(() -> {
                        log.warn("Parent category not found with id: {}", request.parentId());
                        return new IllegalArgumentException("Parent category not found");
                    });
            existing.setParent(parentCategory);
            log.debug("Set new parent {} for category {}", parentCategory.getId(), id);
        } else {
            existing.setParent(null);
            log.debug("Removing parent for category id: {} (moving to top-level)", id);
        }

        Category saved = categoryRepository.save(existing);
        log.info("Successfully updated category with id: {} and name: {}", saved.getId(), saved.getName());
        return categoryMapper.toResponse(saved);
    }

    @Override
    @Transactional
    public void delete(String id) {
        log.info("Deleting category with id: {}", id);
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> {
                    log.warn("Delete failed - category not found with id: {}", id);
                    return new IllegalArgumentException("Category not found");
                });

        try {
            categoryRepository.clearParentForChildren(id);
            categoryRepository.flush();
        } catch (Exception e) {
            log.debug("Direct clearParentForChildren failed for {}, fallback to entity", id, e);
        }
        List<Category> children = categoryRepository.findByParent(category);
        if (!children.isEmpty()) {
            log.info("Category {} has {} children (entity), nullifying parent", id, children.size());
            for (Category child : children) {
                child.setParent(null);
            }
            categoryRepository.saveAll(children);
            categoryRepository.flush();
        }

        try {
            productRepository.clearCategoryForProducts(id);
            productRepository.flush();
        } catch (Exception e) {
            log.debug("Direct clearCategoryForProducts failed for {}", id, e);
        }
        List<Product> products = productRepository.findAllByCategoryId(id);
        if (!products.isEmpty()) {
            log.info("Category {} has {} products (entity), nullifying category", id, products.size());
            for (Product p : products) {
                p.setCategory(null);
            }
            productRepository.saveAll(products);
            productRepository.flush();
        }

        categoryRepository.delete(category);
        categoryRepository.flush();
        log.info("Successfully deleted category with id: {}", id);
    }

    private void assertNoCycle(String categoryId, String newParentId) {
        java.util.Set<String> visited = new java.util.HashSet<>();
        String cur = newParentId;
        while (cur != null) {
            if (!visited.add(cur)) {
                throw new IllegalArgumentException("Cycle detected in category hierarchy");
            }
            if (cur.equals(categoryId)) {
                throw new IllegalArgumentException("Category cannot be ancestor of itself — cycle detected");
            }
            Category p = categoryRepository.findById(cur).orElse(null);
            cur = (p != null && p.getParent() != null) ? p.getParent().getId() : null;
        }
    }
}
