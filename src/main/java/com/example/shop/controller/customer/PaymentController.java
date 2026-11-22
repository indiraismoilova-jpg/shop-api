package com.example.shop.controller.customer;

import com.example.shop.dto.request.PaymentRequest;
import com.example.shop.dto.response.PaymentMethodResponse;
import com.example.shop.dto.response.PaymentResponse;
import com.example.shop.service.PaymentService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
@PreAuthorize("isAuthenticated()")
public class PaymentController {

    private final PaymentService service;

    public PaymentController(PaymentService service) {
        this.service = service;
    }

    @PostMapping("/process")
    public PaymentResponse process(
            Authentication authentication,
            @Valid @RequestBody PaymentRequest request) {

        return service.process(
                authentication.getName(),
                request);
    }

    @GetMapping("/{orderId}/status")
    public PaymentResponse status(
            Authentication authentication,
            @PathVariable Long orderId) {

        return service.status(
                authentication.getName(),
                orderId);
    }

    @GetMapping("/methods")
    public Page<PaymentMethodResponse> methods(
            @PageableDefault(size = 20)
            Pageable pageable) {

        return service.methods(pageable);
    }
}
