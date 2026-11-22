package com.example.shop.controller.admin;

import com.example.shop.dto.request.OrderNoteRequest;
import com.example.shop.dto.request.OrderStatusRequest;
import com.example.shop.dto.request.TrackingRequest;
import com.example.shop.dto.response.OrderResponse;
import com.example.shop.model.enums.OrderStatus;
import com.example.shop.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/admin/orders")
@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
public class AdminOrderController {

    private final OrderService service;

    public AdminOrderController(OrderService service) {
        this.service = service;
    }

    @GetMapping
    public Page<OrderResponse> list(
            @RequestParam(required = false)
            OrderStatus status,

            @RequestParam(required = false)
            Long userId,

            @RequestParam(required = false)
            String orderNumber,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime from,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            LocalDateTime to,

            @PageableDefault(
                    size = 20,
                    sort = "createdAt",
                    direction = Sort.Direction.DESC)
            Pageable pageable) {

        return service.adminList(
                status,
                userId,
                orderNumber,
                from,
                to,
                pageable);
    }

    @PutMapping("/{id}/status")
    public OrderResponse status(
            @PathVariable Long id,
            @Valid @RequestBody OrderStatusRequest request) {

        return service.status(
                id,
                request.status());
    }

    @PutMapping("/{id}/tracking")
    public OrderResponse tracking(
            @PathVariable Long id,
            @Valid @RequestBody TrackingRequest request) {

        return service.tracking(
                id,
                request);
    }

    @PutMapping("/{id}/note")
    public OrderResponse note(
            @PathVariable Long id,
            @Valid @RequestBody OrderNoteRequest request) {

        return service.note(
                id,
                request);
    }
}