package com.example.shop.service;

import com.example.shop.dto.request.CreateProductRequest;
import com.example.shop.dto.request.UpdateProductRequest;
import com.example.shop.dto.response.ProductResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ProductService {

    Page<ProductResponse> list(Pageable pageable);

    ProductResponse getById(Long id);

    ProductResponse create(CreateProductRequest request);

    ProductResponse update(Long id, UpdateProductRequest request);

    void delete(Long id);
}