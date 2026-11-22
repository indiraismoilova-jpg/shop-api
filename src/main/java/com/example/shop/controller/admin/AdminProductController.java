package com.example.shop.controller.admin;

import com.example.shop.dto.request.CreateProductRequest;
import com.example.shop.dto.request.StockRequest;
import com.example.shop.dto.request.UpdateProductRequest;
import com.example.shop.dto.request.VariantRequest;
import com.example.shop.dto.response.ProductResponse;
import com.example.shop.dto.response.VariantResponse;
import com.example.shop.service.CatalogService;
import com.example.shop.service.ProductService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequestMapping("/api/admin/products")
@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
public class AdminProductController {

    private final ProductService products;
    private final CatalogService catalog;

    public AdminProductController(
            ProductService products,
            CatalogService catalog) {

        this.products = products;
        this.catalog = catalog;
    }

    @PostMapping
    public ResponseEntity<ProductResponse> create(
            @Valid @RequestBody CreateProductRequest request) {

        ProductResponse created = products.create(request);

        return ResponseEntity.created(
                        URI.create("/api/products/" + created.id()))
                .body(created);
    }

    @PutMapping("/{id}")
    public ProductResponse update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateProductRequest request) {

        return products.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable Long id) {

        products.delete(id);

        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/stock")
    public ProductResponse stock(
            @PathVariable Long id,
            @Valid @RequestBody StockRequest request) {

        return catalog.stock(id, request);
    }

    @PatchMapping("/{id}/activate")
    public ProductResponse activate(
            @PathVariable Long id) {

        return catalog.active(id, true);
    }

    @PatchMapping("/{id}/deactivate")
    public ProductResponse deactivate(
            @PathVariable Long id) {

        return catalog.active(id, false);
    }

    @PostMapping("/{id}/variants")
    public ResponseEntity<VariantResponse> createVariant(
            @PathVariable Long id,
            @Valid @RequestBody VariantRequest request) {

        VariantResponse created = catalog.saveVariant(
                id,
                null,
                request);

        return ResponseEntity.status(201)
                .header(
                        "Location",
                        "/api/admin/products/" + id
                                + "/variants/" + created.id())
                .body(created);
    }

    @PutMapping("/{id}/variants/{variantId}")
    public VariantResponse updateVariant(
            @PathVariable Long id,
            @PathVariable Long variantId,
            @Valid @RequestBody VariantRequest request) {

        return catalog.saveVariant(
                id,
                variantId,
                request);
    }

    @DeleteMapping("/{id}/variants/{variantId}")
    public ResponseEntity<Void> deleteVariant(
            @PathVariable Long id,
            @PathVariable Long variantId) {

        catalog.deleteVariant(id, variantId);

        return ResponseEntity.noContent().build();
    }
}