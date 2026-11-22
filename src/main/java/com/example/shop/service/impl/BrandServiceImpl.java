package com.example.shop.service.impl;

import com.example.shop.dto.request.CreateBrandRequest;
import com.example.shop.dto.request.UpdateBrandRequest;
import com.example.shop.dto.response.BrandResponse;
import com.example.shop.exception.ResourceNotFoundException;
import com.example.shop.mapper.BrandMapper;
import com.example.shop.model.entity.Brand;
import com.example.shop.repository.BrandRepository;
import com.example.shop.service.BrandService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Set;

@Service
@Transactional(readOnly = true)
public class BrandServiceImpl implements BrandService {

    private final BrandRepository brands;
    private final BrandMapper mapper;

    public BrandServiceImpl(
            BrandRepository brands,
            BrandMapper mapper) {

        this.brands = brands;
        this.mapper = mapper;
    }

    @Override
    public Page<BrandResponse> list(Pageable pageable) {
        Set<String> allowedSorts = Set.of("id", "name", "slug", "country");

        for (var order : pageable.getSort()) {
            if (!allowedSorts.contains(order.getProperty())) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Unsupported brand sort field");
            }
        }

        Pageable limited = PageRequest.of(
                pageable.getPageNumber(),
                Math.min(pageable.getPageSize(), 100),
                pageable.getSort());

        return brands.findByIsActiveTrue(limited)
                .map(mapper::toResponse);
    }

    @Override
    public BrandResponse getById(Long id) {
        Brand brand = brands.findByIdAndIsActiveTrue(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Brand not found"));

        return mapper.toResponse(brand);
    }

    @Override
    @Transactional
    public BrandResponse create(CreateBrandRequest request) {
        if (brands.existsBySlug(request.slug())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Brand slug already exists");
        }

        Brand brand = mapper.toEntity(request);
        brands.saveAndFlush(brand);

        return mapper.toResponse(brand);
    }

    @Override
    @Transactional
    public BrandResponse update(
            Long id,
            UpdateBrandRequest request) {

        Brand brand = findBrand(id);

        if (brands.existsBySlugAndIdNot(request.slug(), id)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Brand slug already exists");
        }

        mapper.update(brand, request);
        brands.saveAndFlush(brand);

        return mapper.toResponse(brand);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Brand brand = findBrand(id);

        brand.setActive(false);
        brands.save(brand);
    }

    private Brand findBrand(Long id) {
        return brands.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Brand not found"));
    }
}
