package com.example.shop.mapper;

import com.example.shop.dto.request.CreateCategoryRequest;
import com.example.shop.dto.request.UpdateCategoryRequest;
import com.example.shop.dto.response.CategoryResponse;
import com.example.shop.dto.response.CategoryTreeResponse;
import com.example.shop.model.entity.Category;
import org.springframework.stereotype.Component;

import java.util.Comparator;

@Component
public class CategoryMapper {

    public Category toEntity(CreateCategoryRequest request) {
        Category category = new Category();

        category.setName(request.name().trim());
        category.setSlug(request.slug());
        category.setDescription(request.description());
        category.setImageUrl(request.imageUrl());
        category.setSortOrder(request.sortOrder());
        category.setActive(request.isActive());

        return category;
    }

    public void update(
            Category category,
            UpdateCategoryRequest request) {

        category.setName(request.name().trim());
        category.setSlug(request.slug());
        category.setDescription(request.description());
        category.setImageUrl(request.imageUrl());
        category.setSortOrder(request.sortOrder());
        category.setActive(request.isActive());
    }

    public CategoryResponse toResponse(Category category) {
        Long parentId = category.getParentCategory() == null
                ? null
                : category.getParentCategory().getId();

        return new CategoryResponse(
                category.getId(),
                category.getName(),
                category.getSlug(),
                category.getDescription(),
                category.getImageUrl(),
                category.getSortOrder(),
                category.isActive(),
                parentId);
    }

    public CategoryTreeResponse toTreeResponse(Category category) {
        var children = category.getChildren().stream()
                .filter(Category::isActive)
                .sorted(Comparator
                        .comparing(Category::getSortOrder)
                        .thenComparing(Category::getId))
                .map(this::toTreeResponse)
                .toList();

        return new CategoryTreeResponse(
                category.getId(),
                category.getName(),
                category.getSlug(),
                category.getImageUrl(),
                category.getSortOrder(),
                children);
    }
}