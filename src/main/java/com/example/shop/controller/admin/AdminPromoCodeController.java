package com.example.shop.controller.admin;

import com.example.shop.dto.request.PromoCodeRequest;
import com.example.shop.dto.response.PromoResponse;
import com.example.shop.service.PromoCodeService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/promo-codes")
@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
public class AdminPromoCodeController {

    private final PromoCodeService service;

    public AdminPromoCodeController(PromoCodeService service) {
        this.service = service;
    }

    @GetMapping
    public Page<PromoResponse> list(
            @PageableDefault(size = 20)
            Pageable pageable) {

        return service.list(pageable);
    }

    @PostMapping
    public ResponseEntity<PromoResponse> create(
            Authentication authentication,
            @Valid @RequestBody PromoCodeRequest request) {

        return ResponseEntity.status(201)
                .body(service.save(
                        null,
                        authentication.getName(),
                        request));
    }

    @PutMapping("/{id}")
    public PromoResponse update(
            Authentication authentication,
            @PathVariable Long id,
            @Valid @RequestBody PromoCodeRequest request) {

        return service.save(
                id,
                authentication.getName(),
                request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable Long id) {

        service.delete(id);

        return ResponseEntity.noContent().build();
    }
}