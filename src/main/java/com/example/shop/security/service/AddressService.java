package com.example.shop.service;

import com.example.shop.dto.request.CreateAddressRequest;
import com.example.shop.dto.request.UpdateAddressRequest;
import com.example.shop.dto.response.AddressResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AddressService {

    Page<AddressResponse> list(String email, Pageable pageable);

    AddressResponse create(
            String email,
            CreateAddressRequest request);

    AddressResponse update(
            String email,
            Long id,
            UpdateAddressRequest request);

    void delete(String email, Long id);

    AddressResponse setDefault(String email, Long id);
}