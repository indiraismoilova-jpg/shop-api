package com.example.shop.service.impl;

import com.example.shop.dto.request.CreateProductRequest;
import com.example.shop.dto.request.UpdateProductRequest;
import com.example.shop.dto.response.ProductResponse;
import com.example.shop.exception.ResourceNotFoundException;
import com.example.shop.mapper.ProductMapper;
import com.example.shop.model.entity.Brand;
import com.example.shop.model.entity.Category;
import com.example.shop.model.entity.Product;
import com.example.shop.repository.BrandRepository;
import com.example.shop.repository.CategoryRepository;
import com.example.shop.repository.ProductRepository;
import com.example.shop.service.ProductService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;

@Service
@Transactional(readOnly = true)
public class ProductServiceImpl implements ProductService {

    private final ProductRepository products;
    private final CategoryRepository categories;
    private final BrandRepository brands;
    private final ProductMapper mapper;

    public ProductServiceImpl(
            ProductRepository products,
            CategoryRepository categories,
            BrandRepository brands,
            ProductMapper mapper) {

        this.products = products;
        this.categories = categories;
        this.brands = brands;
        this.mapper = mapper;
    }

    @Override
    public Page<ProductResponse> list(Pageable pageable) {
        Set<String> allowedSorts = Set.of(
                "id",
                "name",
                "basePrice",
                "averageRating",
                "createdAt",
                "salesCount"
        );

        for (var order : pageable.getSort()) {
            if (!allowedSorts.contains(order.getProperty())) {
                throw badRequest("Unsupported sort field");
            }
        }

        Pageable limited = PageRequest.of(
                pageable.getPageNumber(),
                Math.min(pageable.getPageSize(), 100),
                pageable.getSort()
        );

        return products.findAll(
                (root, query, cb) -> cb.isTrue(root.get("isActive")),
                limited
        ).map(mapper::toResponse);
    }

    @Override
    public ProductResponse getById(Long id) {
        Product product = products.findByIdAndIsActiveTrue(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "No product with id " + id));

        return mapper.toResponse(product);
    }

    @Override
    @Transactional
    public ProductResponse create(CreateProductRequest request) {
        String slug = request.slug().trim();
        String sku = request.sku().trim();

        if (products.existsBySlug(slug)) {
            throw conflict("Product slug already exists");
        }

        if (products.existsBySku(sku)) {
            throw conflict("Product SKU already exists");
        }

        validateDiscount(
                request.basePrice(),
                request.discountPrice(),
                request.discountStartDate(),
                request.discountEndDate()
        );

        Category category = findCategory(request.categoryId());
        Brand brand = findBrand(request.brandId());

        Product product = mapper.toEntity(request);
        product.setCategory(category);
        product.setBrand(brand);

        return mapper.toResponse(products.saveAndFlush(product));
    }

    @Override
    @Transactional
    public ProductResponse update(
            Long id,
            UpdateProductRequest request) {

        Product product = findAny(id);
        String slug = request.slug().trim();
        String sku = request.sku().trim();

        if (products.existsBySlugAndIdNot(slug, id)) {
            throw conflict("Product slug already exists");
        }

        if (products.existsBySkuAndIdNot(sku, id)) {
            throw conflict("Product SKU already exists");
        }

        validateDiscount(
                request.basePrice(),
                request.discountPrice(),
                request.discountStartDate(),
                request.discountEndDate()
        );

        Category category = findCategory(request.categoryId());
        Brand brand = findBrand(request.brandId());

        mapper.update(product, request);
        product.setCategory(category);
        product.setBrand(brand);

        return mapper.toResponse(products.saveAndFlush(product));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Product product = findAny(id);
        product.setActive(false);
        products.saveAndFlush(product);
    }

    private Product findAny(Long id) {
        return products.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "No product with id " + id));
    }

    private Category findCategory(Long id) {
        return categories.findByIdAndIsActiveTrue(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "No active category with id " + id));
    }

    private Brand findBrand(Long id) {
        if (id == null) {
            return null;
        }

        return brands.findByIdAndIsActiveTrue(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "No active brand with id " + id));
    }

    private void validateDiscount(
            BigDecimal basePrice,
            BigDecimal discountPrice,
            LocalDateTime start,
            LocalDateTime end) {

        if (discountPrice == null) {
            if (start != null || end != null) {
                throw badRequest(
                        "Discount dates require a discount price");
            }

            return;
        }

        if (discountPrice.compareTo(basePrice) >= 0) {
            throw badRequest(
                    "Discount price must be lower than base price");
        }

        if (start != null && end != null && !end.isAfter(start)) {
            throw badRequest(
                    "Discount end must be after discount start");
        }
    }

    private ResponseStatusException badRequest(String message) {
        return new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                message
        );
    }

    private ResponseStatusException conflict(String message) {
        return new ResponseStatusException(
                HttpStatus.CONFLICT,
                message
        );
    }
}