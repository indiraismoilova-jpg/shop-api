package com.example.shop.controller.customer;

import com.example.shop.dto.request.DeliveryCalculateRequest;
import com.example.shop.dto.response.DeliveryQuoteResponse;
import com.example.shop.dto.response.DeliveryResponse;
import com.example.shop.service.DeliveryService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/delivery")
@PreAuthorize("isAuthenticated()")
public class DeliveryController {

    private final DeliveryService service;

    public DeliveryController(DeliveryService service) {
        this.service = service;
    }

    @GetMapping("/methods")
    @PreAuthorize("permitAll()")
    public Page<DeliveryResponse> methods(
            @PageableDefault(size = 20)
            Pageable pageable) {

        return service.methods(pageable);
    }

    @PostMapping("/calculate")
    public DeliveryQuoteResponse calculate(
            @Valid @RequestBody DeliveryCalculateRequest request) {

        return service.calculate(
                request.deliveryOptionId(),
                request.subtotal());
    }
}