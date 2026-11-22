package com.example.shop.mapper;

import com.example.shop.dto.request.CreateBrandRequest;
import com.example.shop.dto.request.UpdateBrandRequest;
import com.example.shop.dto.response.BrandResponse;
import com.example.shop.model.entity.Brand;
import org.springframework.stereotype.Component;

@Component
public class BrandMapper {

    public Brand toEntity(CreateBrandRequest request) {
        Brand brand = new Brand();

        brand.setName(request.name().trim());
        brand.setSlug(request.slug());
        brand.setDescription(request.description());
        brand.setLogoUrl(request.logoUrl());
        brand.setWebsiteUrl(request.websiteUrl());
        brand.setCountry(request.country());
        brand.setActive(request.isActive());

        return brand;
    }

    public void update(
            Brand brand,
            UpdateBrandRequest request) {

        brand.setName(request.name().trim());
        brand.setSlug(request.slug());
        brand.setDescription(request.description());
        brand.setLogoUrl(request.logoUrl());
        brand.setWebsiteUrl(request.websiteUrl());
        brand.setCountry(request.country());
        brand.setActive(request.isActive());
    }

    public BrandResponse toResponse(Brand brand) {
        return new BrandResponse(
                brand.getId(),
                brand.getName(),
                brand.getSlug(),
                brand.getDescription(),
                brand.getLogoUrl(),
                brand.getWebsiteUrl(),
                brand.getCountry(),
                brand.isActive());
    }
}