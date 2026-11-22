package com.example.shop.controller.customer;

import com.example.shop.dto.request.CartItemRequest;
import com.example.shop.dto.request.MergeCartRequest;
import com.example.shop.dto.request.PromoApplyRequest;
import com.example.shop.dto.request.QuantityRequest;
import com.example.shop.dto.response.CartResponse;
import com.example.shop.service.CartService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cart")
@PreAuthorize("isAuthenticated()")
public class CartController {

    private final CartService service;

    public CartController(CartService service) {
        this.service = service;
    }

    @GetMapping
    public CartResponse get(
            Authentication authentication,
            @PageableDefault(size = 20)
            Pageable pageable) {

        return service.get(
                authentication.getName(),
                pageable);
    }

    @PostMapping("/items")
    public ResponseEntity<CartResponse> add(
            Authentication authentication,
            @Valid @RequestBody CartItemRequest request,
            @PageableDefault(size = 20)
            Pageable pageable) {

        CartResponse cart = service.add(
                authentication.getName(),
                request,
                pageable);

        return ResponseEntity.status(201)
                .header("Location", "/api/cart")
                .body(cart);
    }

    @PutMapping("/items/{itemId}")
    public CartResponse update(
            Authentication authentication,
            @PathVariable Long itemId,
            @Valid @RequestBody QuantityRequest request,
            @PageableDefault(size = 20)
            Pageable pageable) {

        return service.update(
                authentication.getName(),
                itemId,
                request,
                pageable);
    }

    @DeleteMapping("/items/{itemId}")
    public ResponseEntity<Void> remove(
            Authentication authentication,
            @PathVariable Long itemId) {

        service.remove(
                authentication.getName(),
                itemId);

        return ResponseEntity.noContent().build();
    }

    @DeleteMapping
    public ResponseEntity<Void> clear(
            Authentication authentication) {

        service.clear(authentication.getName());

        return ResponseEntity.noContent().build();
    }

    @PostMapping("/apply-promo")
    public CartResponse apply(
            Authentication authentication,
            @Valid @RequestBody PromoApplyRequest request,
            @PageableDefault(size = 20)
            Pageable pageable) {

        return service.apply(
                authentication.getName(),
                request.code(),
                pageable);
    }

    @DeleteMapping("/promo")
    public CartResponse removePromo(
            Authentication authentication,
            @PageableDefault(size = 20)
            Pageable pageable) {

        return service.removePromo(
                authentication.getName(),
                pageable);
    }

    @PostMapping("/merge")
    public CartResponse merge(
            Authentication authentication,
            @Valid @RequestBody MergeCartRequest request,
            @PageableDefault(size = 20)
            Pageable pageable) {

        return service.merge(
                authentication.getName(),
                request,
                pageable);
    }
}