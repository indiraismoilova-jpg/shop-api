package com.example.shop.service;

import com.example.shop.dto.request.CreateBrandRequest;
import com.example.shop.dto.request.UpdateBrandRequest;
import com.example.shop.dto.response.BrandResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface BrandService {

    Page<BrandResponse> list(Pageable pageable);

    BrandResponse getById(Long id);

    BrandResponse create(CreateBrandRequest request);

    BrandResponse update(Long id, UpdateBrandRequest request);

    void delete(Long id);
}