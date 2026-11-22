package com.example.shop.repository;

import com.example.shop.model.entity.Category;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CategoryRepository
        extends JpaRepository<Category, Long> {

    Page<Category> findByIsActiveTrue(Pageable pageable);

    Page<Category> findByParentCategoryIsNullAndIsActiveTrue(
            Pageable pageable);

    Page<Category> findByParentCategory_IdAndIsActiveTrue(
            Long parentId,
            Pageable pageable);

    Optional<Category> findByIdAndIsActiveTrue(Long id);

    Optional<Category> findBySlugAndIsActiveTrue(String slug);

    boolean existsBySlug(String slug);

    boolean existsBySlugAndIdNot(String slug, Long id);

    boolean existsByParentCategory_Id(Long parentId);
}
