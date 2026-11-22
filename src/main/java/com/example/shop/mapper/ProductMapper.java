package com.example.shop.mapper;

import com.example.shop.dto.request.CreateProductRequest;
import com.example.shop.dto.request.UpdateProductRequest;
import com.example.shop.dto.response.ProductResponse;
import com.example.shop.model.entity.Product;
import org.springframework.stereotype.Component;

@Component
public class ProductMapper {

    private final CategoryMapper categoryMapper;
    private final BrandMapper brandMapper;

    public ProductMapper(
            CategoryMapper categoryMapper,
            BrandMapper brandMapper) {

        this.categoryMapper = categoryMapper;
        this.brandMapper = brandMapper;
    }

    public Product toEntity(CreateProductRequest request) {
        Product product = new Product();

        product.setName(request.name().trim());
        product.setSlug(request.slug().trim());
        product.setDescription(request.description());
        product.setShortDescription(request.shortDescription());
        product.setSku(request.sku().trim());
        product.setBasePrice(request.basePrice());
        product.setDiscountPrice(request.discountPrice());
        product.setDiscountStartDate(request.discountStartDate());
        product.setDiscountEndDate(request.discountEndDate());
        product.setStockQuantity(request.stockQuantity());
        product.setLowStockThreshold(request.lowStockThreshold());
        product.setWeight(request.weight());
        product.setDimensions(request.dimensions());
        product.setActive(request.isActive());
        product.setFeatured(request.isFeatured());
        product.setNewArrival(request.isNewArrival());
        product.setMetaTitle(request.metaTitle());
        product.setMetaDescription(request.metaDescription());

        return product;
    }

    public void update(
            Product product,
            UpdateProductRequest request) {

        product.setName(request.name().trim());
        product.setSlug(request.slug().trim());
        product.setDescription(request.description());
        product.setShortDescription(request.shortDescription());
        product.setSku(request.sku().trim());
        product.setBasePrice(request.basePrice());
        product.setDiscountPrice(request.discountPrice());
        product.setDiscountStartDate(request.discountStartDate());
        product.setDiscountEndDate(request.discountEndDate());
        product.setLowStockThreshold(request.lowStockThreshold());
        product.setWeight(request.weight());
        product.setDimensions(request.dimensions());
        product.setActive(request.isActive());
        product.setFeatured(request.isFeatured());
        product.setNewArrival(request.isNewArrival());
        product.setMetaTitle(request.metaTitle());
        product.setMetaDescription(request.metaDescription());
    }

    public ProductResponse toResponse(Product product) {
        return new ProductResponse(
                product.getId(),
                product.getName(),
                product.getSlug(),
                product.getDescription(),
                product.getShortDescription(),
                product.getSku(),
                product.getBasePrice(),
                product.getDiscountPrice(),
                product.getDiscountStartDate(),
                product.getDiscountEndDate(),
                product.getStockQuantity(),
                product.getLowStockThreshold(),
                product.getWeight(),
                product.getDimensions(),
                product.isActive(),
                product.isFeatured(),
                product.isNewArrival(),
                product.getAverageRating(),
                product.getReviewCount(),
                product.getViewCount(),
                product.getSalesCount(),
                product.getMetaTitle(),
                product.getMetaDescription(),
                product.getCreatedAt(),
                product.getUpdatedAt(),
                categoryMapper.toResponse(product.getCategory()),
                product.getBrand() == null
                        ? null
                        : brandMapper.toResponse(product.getBrand())
        );
    }
}