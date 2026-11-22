package com.example.shop.controller.customer;

import com.example.shop.dto.response.BrandResponse;
import com.example.shop.dto.response.ProductResponse;
import com.example.shop.service.BrandService;
import com.example.shop.service.CatalogService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/brands")
public class BrandController {

    private final BrandService brands;
    private final CatalogService catalog;

    public BrandController(
            BrandService brands,
            CatalogService catalog) {

        this.brands = brands;
        this.catalog = catalog;
    }

    @GetMapping
    public Page<BrandResponse> list(
            @PageableDefault(
                    size = 20,
                    sort = "name")
            Pageable pageable) {

        return brands.list(pageable);
    }

    @GetMapping("/{id}")
    public BrandResponse getById(
            @PathVariable Long id) {

        return brands.getById(id);
    }

    @GetMapping("/{id}/products")
    public Page<ProductResponse> products(
            @PathVariable Long id,
            @PageableDefault(size = 20)
            Pageable pageable) {

        return catalog.search(
                null,
                id,
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