package com.example.shop.controller.customer;

import com.example.shop.dto.request.CreateAddressRequest;
import com.example.shop.dto.request.UpdateAddressRequest;
import com.example.shop.dto.response.AddressResponse;
import com.example.shop.service.AddressService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequestMapping("/api/users/me/addresses")
public class AddressController {

    private final AddressService addresses;

    public AddressController(AddressService addresses) {
        this.addresses = addresses;
    }

    @GetMapping
    public Page<AddressResponse> list(
            Authentication authentication,
            @PageableDefault(size = 20, sort = "id")
            Pageable pageable) {

        return addresses.list(
                authentication.getName(),
                pageable);
    }

    @PostMapping
    public ResponseEntity<AddressResponse> create(
            Authentication authentication,
            @Valid @RequestBody CreateAddressRequest request) {

        AddressResponse created = addresses.create(
                authentication.getName(),
                request);

        return ResponseEntity.created(
                        URI.create("/api/users/me/addresses/" + created.id()))
                .body(created);
    }

    @PutMapping("/{id}")
    public AddressResponse update(
            Authentication authentication,
            @PathVariable Long id,
            @Valid @RequestBody UpdateAddressRequest request) {

        return addresses.update(
                authentication.getName(),
                id,
                request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            Authentication authentication,
            @PathVariable Long id) {

        addresses.delete(authentication.getName(), id);

        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/default")
    public AddressResponse setDefault(
            Authentication authentication,
            @PathVariable Long id) {

        return addresses.setDefault(
                authentication.getName(),
                id);
    }
}