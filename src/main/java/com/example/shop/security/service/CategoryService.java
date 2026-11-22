package com.example.shop.service;

import com.example.shop.dto.request.CreateCategoryRequest;
import com.example.shop.dto.request.ReorderCategoriesRequest;
import com.example.shop.dto.request.UpdateCategoryRequest;
import com.example.shop.dto.response.CategoryResponse;
import com.example.shop.dto.response.CategoryTreeResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface CategoryService {

    Page<CategoryResponse> list(Pageable pageable);

    Page<CategoryTreeResponse> tree(Pageable pageable);

    CategoryResponse getById(Long id);

    CategoryResponse getBySlug(String slug);

    CategoryResponse create(CreateCategoryRequest request);

    CategoryResponse update(Long id, UpdateCategoryRequest request);

    void delete(Long id);

    void reorder(ReorderCategoriesRequest request);
}