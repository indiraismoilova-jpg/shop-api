package com.example.shop.controller.customer;

import com.example.shop.dto.response.ProductResponse;
import com.example.shop.dto.response.VariantResponse;
import com.example.shop.service.CatalogService;
import com.example.shop.service.ProductService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService products;
    private final CatalogService catalog;

    public ProductController(
            ProductService products,
            CatalogService catalog) {

        this.products = products;
        this.catalog = catalog;
    }

    @GetMapping({"", "/search"})
    public Page<ProductResponse> list(
            @RequestParam(required = false) Long category,
            @RequestParam(required = false) Long brand,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Long brandId,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) Boolean inStock,
            @RequestParam(required = false) BigDecimal rating,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String q,
            @PageableDefault(
                    size = 20,
                    sort = "createdAt",
                    direction = Sort.Direction.DESC)
            Pageable pageable) {

        return catalog.search(
                categoryId == null ? category : categoryId,
                brandId == null ? brand : brandId,
                minPrice,
                maxPrice,
                inStock,
                rating,
                search == null ? q : search,
                null,
                null,
                pageable);
    }

    @GetMapping("/{id}")
    public ProductResponse getById(
            @PathVariable Long id) {

        return products.getById(id);
    }

    @GetMapping("/{id}/variants")
    public Page<VariantResponse> variants(
            @PathVariable Long id,
            @PageableDefault(size = 20)
            Pageable pageable) {

        return catalog.variants(id, pageable);
    }

    @GetMapping("/{id}/related")
    public Page<ProductResponse> related(
            @PathVariable Long id,
            @PageableDefault(size = 20)
            Pageable pageable) {

        return catalog.search(
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                id,
                pageable);
    }

    @GetMapping("/featured")
    public Page<ProductResponse> featured(
            @PageableDefault(size = 20)
            Pageable pageable) {

        return catalog.search(
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                "featured",
                null,
                pageable);
    }

    @GetMapping("/new-arrivals")
    public Page<ProductResponse> arrivals(
            @PageableDefault(
                    size = 20,
                    sort = "createdAt",
                    direction = Sort.Direction.DESC)
            Pageable pageable) {

        return catalog.search(
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                "new-arrivals",
                null,
                pageable);
    }

    @GetMapping("/best-sellers")
    public Page<ProductResponse> bestSellers(
            @PageableDefault(size = 20)
            Pageable pageable) {

        return catalog.search(
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                "best-sellers",
                null,
                pageable);
    }
}