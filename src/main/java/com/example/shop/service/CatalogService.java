package com.example.shop.service;

import com.example.shop.dto.request.StockRequest;
import com.example.shop.dto.request.VariantRequest;
import com.example.shop.dto.response.ProductResponse;
import com.example.shop.dto.response.VariantResponse;
import com.example.shop.exception.ResourceNotFoundException;
import com.example.shop.mapper.ProductMapper;
import com.example.shop.model.entity.Brand;
import com.example.shop.model.entity.Category;
import com.example.shop.model.entity.Product;
import com.example.shop.model.entity.ProductVariant;
import com.example.shop.repository.ProductRepository;
import com.example.shop.repository.ProductVariantRepository;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import static com.example.shop.service.CommerceSupport.bad;
import static com.example.shop.service.CommerceSupport.conflict;

@Service
@Transactional(readOnly = true)
public class CatalogService {

    private final ProductRepository products;
    private final ProductVariantRepository variants;
    private final ProductMapper mapper;
    private final CommerceSupport support;

    public CatalogService(
            ProductRepository products,
            ProductVariantRepository variants,
            ProductMapper mapper,
            CommerceSupport support) {

        this.products = products;
        this.variants = variants;
        this.mapper = mapper;
        this.support = support;
    }

    public Page<ProductResponse> search(
            Long category,
            Long brand,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            Boolean inStock,
            BigDecimal rating,
            String search,
            String selection,
            Long relatedId,
            Pageable pageable) {

        if ((minPrice != null && minPrice.signum() < 0)
                || (maxPrice != null && maxPrice.signum() < 0)
                || (minPrice != null
                && maxPrice != null
                && minPrice.compareTo(maxPrice) > 0)) {

            throw bad("Invalid price range");
        }

        if (rating != null
                && (rating.signum() < 0
                || rating.compareTo(BigDecimal.valueOf(5)) > 0)) {

            throw bad("Rating must be between 0 and 5");
        }

        Long relatedCategory = relatedId == null
                ? null
                : support.activeProduct(relatedId)
                .getCategory().getId();

        if (category != null
                && !support.find(Category.class, category).isActive()) {

            throw new ResourceNotFoundException(
                    "Category not found");
        }

        if (brand != null
                && !support.find(Brand.class, brand).isActive()) {

            throw new ResourceNotFoundException(
                    "Brand not found");
        }

        Pageable page = support.page(
                pageable,
                "id",
                "name",
                "basePrice",
                "averageRating",
                "createdAt",
                "salesCount");

        if ("best-sellers".equals(selection)) {
            page = PageRequest.of(
                    page.getPageNumber(),
                    page.getPageSize(),
                    Sort.by(Sort.Direction.DESC, "salesCount")
                            .and(Sort.by("id")));
        }

        return products.findAll(
                        (root, query, builder) -> {
                            List<Predicate> terms = new ArrayList<>();

                            terms.add(builder.isTrue(
                                    root.get("isActive")));

                            if (category != null) {
                                terms.add(builder.equal(
                                        root.get("category").get("id"),
                                        category));
                            }

                            if (brand != null) {
                                terms.add(builder.equal(
                                        root.get("brand").get("id"),
                                        brand));
                            }

                            LocalDateTime now = LocalDateTime.now();

                            var onSale = builder.and(
                                    builder.isNotNull(
                                            root.get("discountPrice")),
                                    builder.or(
                                            builder.isNull(
                                                    root.get("discountStartDate")),
                                            builder.lessThanOrEqualTo(
                                                    root.get("discountStartDate"),
                                                    now)),
                                    builder.or(
                                            builder.isNull(
                                                    root.get("discountEndDate")),
                                            builder.greaterThan(
                                                    root.get("discountEndDate"),
                                                    now)));

                            var effectivePrice = builder
                                    .<BigDecimal>selectCase()
                                    .when(
                                            onSale,
                                            root.<BigDecimal>get("discountPrice"))
                                    .otherwise(
                                            root.<BigDecimal>get("basePrice"));

                            if (minPrice != null) {
                                terms.add(
                                        builder.greaterThanOrEqualTo(
                                                effectivePrice,
                                                minPrice));
                            }

                            if (maxPrice != null) {
                                terms.add(
                                        builder.lessThanOrEqualTo(
                                                effectivePrice,
                                                maxPrice));
                            }

                            if (inStock != null) {
                                var available = builder.diff(
                                        root.<Integer>get("stockQuantity"),
                                        root.<Integer>get("reservedQuantity"));

                                terms.add(
                                        inStock
                                                ? builder.gt(available, 0)
                                                : builder.le(available, 0));
                            }

                            if (rating != null) {
                                terms.add(
                                        builder.greaterThanOrEqualTo(
                                                root.get("averageRating"),
                                                rating));
                            }

                            if (search != null && !search.isBlank()) {
                                String term = "%"
                                        + search.toLowerCase(Locale.ROOT)
                                        .replace("!", "!!")
                                        .replace("%", "!%")
                                        .replace("_", "!_")
                                        + "%";

                                terms.add(builder.or(
                                        builder.like(
                                                builder.lower(root.get("name")),
                                                term,
                                                '!'),
                                        builder.like(
                                                builder.lower(
                                                        root.get("description")),
                                                term,
                                                '!')));
                            }

                            if ("featured".equals(selection)) {
                                terms.add(builder.isTrue(
                                        root.get("isFeatured")));
                            }

                            if ("new-arrivals".equals(selection)) {
                                terms.add(builder.isTrue(
                                        root.get("isNewArrival")));
                            }

                            if (relatedId != null) {
                                terms.add(builder.equal(
                                        root.get("category").get("id"),
                                        relatedCategory));

                                terms.add(builder.notEqual(
                                        root.get("id"),
                                        relatedId));
                            }

                            return builder.and(
                                    terms.toArray(Predicate[]::new));
                        },
                        page)
                .map(mapper::toResponse);
    }

    public Page<VariantResponse> variants(
            Long productId,
            Pageable pageable) {

        support.activeProduct(productId);

        return variants.findByProductIdAndIsActiveTrue(
                        productId,
                        support.page(
                                pageable,
                                "id",
                                "name",
                                "price"))
                .map(this::variantResponse);
    }

    @Transactional
    public ProductResponse stock(
            Long id,
            StockRequest request) {

        Product product = support.lock(Product.class, id);

        if (request.stockQuantity() < product.getReservedQuantity()) {
            throw conflict(
                    "Stock cannot be less than reserved quantity");
        }

        product.setStockQuantity(request.stockQuantity());

        return mapper.toResponse(product);
    }

    @Transactional
    public ProductResponse active(
            Long id,
            boolean active) {

        Product product = support.lock(Product.class, id);
        product.setActive(active);

        return mapper.toResponse(product);
    }

    @Transactional
    public VariantResponse saveVariant(
            Long productId,
            Long id,
            VariantRequest request) {

        Product product = support.lock(
                Product.class,
                productId);

        if (variants.existsBySkuAndIdNot(
                request.sku(),
                id == null ? -1L : id)) {

            throw conflict("Variant SKU already exists");
        }

        ProductVariant variant = id == null
                ? new ProductVariant()
                : support.lock(ProductVariant.class, id);

        if (id != null
                && !variant.getProduct().getId().equals(productId)) {

            throw new ResourceNotFoundException(
                    "Variant not found");
        }

        if (request.stockQuantity() < variant.getReservedQuantity()) {
            throw conflict(
                    "Stock cannot be less than reserved quantity");
        }

        variant.setProduct(product);
        variant.setSku(request.sku());
        variant.setName(request.name());
        variant.setPrice(request.price());
        variant.setStockQuantity(request.stockQuantity());
        variant.setImageUrl(request.imageUrl());

        variant.setAttributes(
                request.attributes() == null
                        ? new HashMap<>()
                        : new HashMap<>(request.attributes()));

        variant.setActive(request.isActive());

        return variantResponse(
                variants.saveAndFlush(variant));
    }

    @Transactional
    public void deleteVariant(
            Long productId,
            Long id) {

        support.lock(Product.class, productId);

        ProductVariant variant = support.lock(
                ProductVariant.class,
                id);

        if (!variant.getProduct().getId().equals(productId)) {
            throw new ResourceNotFoundException(
                    "Variant not found");
        }

        variant.setActive(false);
    }

    private VariantResponse variantResponse(
            ProductVariant variant) {

        return new VariantResponse(
                variant.getId(),
                variant.getProduct().getId(),
                variant.getSku(),
                variant.getName(),
                variant.getPrice(),
                variant.getStockQuantity(),
                variant.getReservedQuantity(),
                variant.isActive(),
                variant.getImageUrl(),
                new HashMap<>(variant.getAttributes()));
    }
}