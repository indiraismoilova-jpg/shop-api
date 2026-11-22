package com.example.shop.service.impl;
import com.example.shop.dto.response.CategoryTreeResponse;
import org.springframework.data.domain.Sort;
import com.example.shop.dto.request.CreateCategoryRequest;
import com.example.shop.dto.request.UpdateCategoryRequest;
import com.example.shop.dto.response.CategoryResponse;
import com.example.shop.exception.ResourceNotFoundException;
import com.example.shop.mapper.CategoryMapper;
import com.example.shop.model.entity.Category;
import com.example.shop.repository.CategoryRepository;
import com.example.shop.service.CategoryService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import com.example.shop.dto.request.ReorderCategoriesRequest;
import java.util.HashSet;
import java.util.Set;

@Service
@Transactional(readOnly = true)
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categories;
    private final CategoryMapper mapper;

    public CategoryServiceImpl(
            CategoryRepository categories,
            CategoryMapper mapper) {

        this.categories = categories;
        this.mapper = mapper;
    }

    @Override
    public Page<CategoryResponse> list(Pageable pageable) {
        Set<String> allowedSorts =
                Set.of("id", "name", "slug", "sortOrder");

        for (var order : pageable.getSort()) {
            if (!allowedSorts.contains(order.getProperty())) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Unsupported category sort field");
            }
        }

        Pageable limited = PageRequest.of(
                pageable.getPageNumber(),
                Math.min(pageable.getPageSize(), 100),
                pageable.getSort());

        return categories.findByIsActiveTrue(limited)
                .map(mapper::toResponse);
    }

    @Override
    public CategoryResponse getById(Long id) {
        return mapper.toResponse(findActive(id));
    }

    @Override
    public CategoryResponse getBySlug(String slug) {
        Category category = categories.findBySlugAndIsActiveTrue(slug)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Category not found"));

        return mapper.toResponse(category);
    }

    @Override
    @Transactional
    public CategoryResponse create(CreateCategoryRequest request) {
        if (categories.existsBySlug(request.slug())) {
            throw conflict("Category slug already exists");
        }

        Category parent = resolveParent(
                request.parentCategoryId(),
                null);

        Category category = mapper.toEntity(request);
        category.setParentCategory(parent);

        categories.saveAndFlush(category);

        return mapper.toResponse(category);
    }

    @Override
    @Transactional
    public CategoryResponse update(
            Long id,
            UpdateCategoryRequest request) {

        Category category = findAny(id);

        if (categories.existsBySlugAndIdNot(request.slug(), id)) {
            throw conflict("Category slug already exists");
        }

        if (!request.isActive()
                && categories.existsByParentCategory_Id(id)) {
            throw conflict(
                    "Move child categories before deactivating this category");
        }

        Category parent = resolveParent(
                request.parentCategoryId(),
                id);

        mapper.update(category, request);
        category.setParentCategory(parent);

        categories.saveAndFlush(category);

        return mapper.toResponse(category);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Category category = findAny(id);

        if (categories.existsByParentCategory_Id(id)) {
            throw conflict(
                    "Move child categories before deleting this category");
        }

        category.setActive(false);
        categories.save(category);
    }
    @Override
    public Page<CategoryTreeResponse> tree(Pageable pageable) {
        Pageable limited = PageRequest.of(
                pageable.getPageNumber(),
                Math.min(pageable.getPageSize(), 100),
                Sort.by("sortOrder").ascending()
                        .and(Sort.by("id").ascending()));

        return categories
                .findByParentCategoryIsNullAndIsActiveTrue(limited)
                .map(mapper::toTreeResponse);
    }
    @Override
    @Transactional
    public void reorder(ReorderCategoriesRequest request) {
        Set<Long> ids = new HashSet<>();

        for (var item : request.items()) {
            if (!ids.add(item.categoryId())) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Category ids must not repeat");
            }
        }

        var found = categories.findAllById(ids);

        if (found.size() != ids.size()) {
            throw new ResourceNotFoundException(
                    "One or more categories were not found");
        }

        for (var category : found) {
            for (var item : request.items()) {
                if (category.getId().equals(item.categoryId())) {
                    category.setSortOrder(item.sortOrder());
                    break;
                }
            }
        }

        categories.saveAllAndFlush(found);
    }
    private Category resolveParent(
            Long parentId,
            Long currentId) {

        if (parentId == null) {
            return null;
        }

        Category parent = findActive(parentId);
        Category ancestor = parent;
        Set<Long> visited = new HashSet<>();

        while (ancestor != null) {
            if (!visited.add(ancestor.getId())
                    || ancestor.getId().equals(currentId)) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Category hierarchy cannot contain a cycle");
            }

            ancestor = ancestor.getParentCategory();
        }

        return parent;
    }

    private Category findActive(Long id) {
        return categories.findByIdAndIsActiveTrue(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Category not found"));
    }

    private Category findAny(Long id) {
        return categories.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Category not found"));
    }

    private ResponseStatusException conflict(String message) {
        return new ResponseStatusException(
                HttpStatus.CONFLICT,
                message);
    }
}