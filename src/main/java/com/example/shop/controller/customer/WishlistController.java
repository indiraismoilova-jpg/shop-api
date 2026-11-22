package com.example.shop.controller.customer;

import com.example.shop.dto.response.CartResponse;
import com.example.shop.dto.response.WishlistResponse;
import com.example.shop.service.WishlistService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/wishlist")
@PreAuthorize("isAuthenticated()")
public class WishlistController {

    private final WishlistService service;

    public WishlistController(WishlistService service) {
        this.service = service;
    }

    @GetMapping
    public Page<WishlistResponse> list(
            Authentication authentication,
            @PageableDefault(size = 20)
            Pageable pageable) {

        return service.list(
                authentication.getName(),
                pageable);
    }

    @PostMapping("/{productId}")
    public ResponseEntity<WishlistResponse> add(
            Authentication authentication,
            @PathVariable Long productId) {

        return ResponseEntity.status(201)
                .body(service.add(
                        authentication.getName(),
                        productId));
    }

    @DeleteMapping("/{productId}")
    public ResponseEntity<Void> delete(
            Authentication authentication,
            @PathVariable Long productId) {

        service.delete(
                authentication.getName(),
                productId);

        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{productId}/move-to-cart")
    public CartResponse move(
            Authentication authentication,
            @PathVariable Long productId,
            @PageableDefault(size = 20)
            Pageable pageable) {

        return service.move(
                authentication.getName(),
                productId,
                pageable);
    }
}