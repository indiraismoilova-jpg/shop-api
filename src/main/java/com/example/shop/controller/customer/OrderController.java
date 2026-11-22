package com.example.shop.controller.customer;

import com.example.shop.dto.request.CreateOrderRequest;
import com.example.shop.dto.response.CartResponse;
import com.example.shop.dto.response.OrderResponse;
import com.example.shop.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/orders")
@PreAuthorize("isAuthenticated()")
public class OrderController {

    private final OrderService service;

    public OrderController(OrderService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<OrderResponse> create(
            Authentication authentication,
            @Valid @RequestBody CreateOrderRequest request) {

        return ResponseEntity.status(201)
                .body(service.create(
                        authentication.getName(),
                        request));
    }

    @GetMapping
    public Page<OrderResponse> list(
            Authentication authentication,
            @PageableDefault(
                    size = 20,
                    sort = "createdAt",
                    direction = Sort.Direction.DESC)
            Pageable pageable) {

        return service.list(
                authentication.getName(),
                pageable);
    }

    @GetMapping("/{identifier}")
    public OrderResponse get(
            Authentication authentication,
            @PathVariable String identifier) {

        return service.get(
                authentication.getName(),
                identifier);
    }

    @GetMapping("/number/{orderNumber}")
    public OrderResponse getByNumber(
            Authentication authentication,
            @PathVariable String orderNumber) {

        return service.get(
                authentication.getName(),
                orderNumber);
    }

    @PutMapping("/{id}/cancel")
    public OrderResponse cancel(
            Authentication authentication,
            @PathVariable Long id) {

        return service.cancel(
                authentication.getName(),
                id);
    }

    @PostMapping("/{id}/reorder")
    public CartResponse reorder(
            Authentication authentication,
            @PathVariable Long id,
            @PageableDefault(size = 20)
            Pageable pageable) {

        return service.reorder(
                authentication.getName(),
                id,
                pageable);
    }
}