package com.example.shop.controller.customer;

import com.example.shop.dto.request.PromoApplyRequest;
import com.example.shop.dto.response.PromoValidationResponse;
import com.example.shop.service.PromoCodeService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/promo")
@PreAuthorize("isAuthenticated()")
public class PromoCodeController {

    private final PromoCodeService service;

    public PromoCodeController(PromoCodeService service) {
        this.service = service;
    }

    @PostMapping("/validate")
    public PromoValidationResponse validate(
            Authentication authentication,
            @Valid @RequestBody PromoApplyRequest request) {

        return service.validate(
                authentication.getName(),
                request.code());
    }
}