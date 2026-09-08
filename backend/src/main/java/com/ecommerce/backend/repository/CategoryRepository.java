package com.ecommerce.backend.repository;

import com.ecommerce.backend.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface CategoryRepository extends JpaRepository<Category, String> {

    @Query("SELECT c FROM Category c WHERE c.parent IS NULL")
    List<Category> findByParentIdIsNull();

    List<Category> findByParent(Category parent);

    @org.springframework.data.jpa.repository.Modifying
    @Query("UPDATE Category c SET c.parent = null WHERE c.parent.id = :parentId")
    void clearParentForChildren(@org.springframework.data.repository.query.Param("parentId") String parentId);
}
