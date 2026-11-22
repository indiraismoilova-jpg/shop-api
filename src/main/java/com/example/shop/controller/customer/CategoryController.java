package com.example.shop.controller.customer;

import com.example.shop.dto.response.CategoryResponse;
import com.example.shop.dto.response.CategoryTreeResponse;
import com.example.shop.dto.response.ProductResponse;
import com.example.shop.service.CatalogService;
import com.example.shop.service.CategoryService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/categories")
public class CategoryController {

    private final CategoryService categories;
    private final CatalogService catalog;

    public CategoryController(
            CategoryService categories,
            CatalogService catalog) {

        this.categories = categories;
        this.catalog = catalog;
    }

    @GetMapping
    public Page<CategoryTreeResponse> list(
            @PageableDefault(size = 20)
            Pageable pageable) {

        return categories.tree(pageable);
    }

    @GetMapping("/tree")
    public Page<CategoryTreeResponse> tree(
            @PageableDefault(size = 20)
            Pageable pageable) {

        return categories.tree(pageable);
    }

    @GetMapping("/{identifier}")
    public CategoryResponse get(
            @PathVariable String identifier) {

        if (identifier.matches("[0-9]+")) {
            try {
                return categories.getById(
                        Long.parseLong(identifier));

            } catch (NumberFormatException exception) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Invalid category id");
            }
        }

        return categories.getBySlug(identifier);
    }

    @GetMapping("/slug/{slug}")
    public CategoryResponse getBySlug(
            @PathVariable String slug) {

        return categories.getBySlug(slug);
    }

    @GetMapping("/{id}/products")
    public Page<ProductResponse> products(
            @PathVariable Long id,
            @PageableDefault(size = 20)
            Pageable pageable) {

        return catalog.search(
                id,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                pageable);
    }
}