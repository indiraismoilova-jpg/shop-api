package com.example.shop.service.impl;

import com.example.shop.dto.request.CreateAddressRequest;
import com.example.shop.dto.request.UpdateAddressRequest;
import com.example.shop.dto.response.AddressResponse;
import com.example.shop.exception.ResourceNotFoundException;
import com.example.shop.exception.UnauthorizedException;
import com.example.shop.mapper.AddressMapper;
import com.example.shop.model.entity.Address;
import com.example.shop.model.entity.User;
import com.example.shop.repository.AddressRepository;
import com.example.shop.repository.UserRepository;
import com.example.shop.service.AddressService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Set;

@Service
@Transactional(readOnly = true)
public class AddressServiceImpl implements AddressService {

    private final AddressRepository addresses;
    private final UserRepository users;
    private final AddressMapper mapper;

    public AddressServiceImpl(
            AddressRepository addresses,
            UserRepository users,
            AddressMapper mapper) {

        this.addresses = addresses;
        this.users = users;
        this.mapper = mapper;
    }

    @Override
    public Page<AddressResponse> list(
            String email,
            Pageable pageable) {

        User user = currentUser(email, false);

        Set<String> allowedSorts =
                Set.of("id", "title", "country", "city");

        for (var order : pageable.getSort()) {
            if (!allowedSorts.contains(order.getProperty())) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Unsupported address sort field");
            }
        }

        Pageable limited = PageRequest.of(
                pageable.getPageNumber(),
                Math.min(pageable.getPageSize(), 100),
                pageable.getSort());

        return addresses.findByUser_Id(user.getId(), limited)
                .map(mapper::toResponse);
    }

    @Override
    @Transactional
    public AddressResponse create(
            String email,
            CreateAddressRequest request) {

        User user = currentUser(email, true);

        if (request.isDefault()) {
            addresses.clearOtherDefaults(user.getId(), null);
        }

        Address address = mapper.toEntity(request);
        address.setUser(user);

        addresses.saveAndFlush(address);

        return mapper.toResponse(address);
    }

    @Override
    @Transactional
    public AddressResponse update(
            String email,
            Long id,
            UpdateAddressRequest request) {

        User user = currentUser(email, true);
        Address address = ownedAddress(id, user.getId());

        if (request.isDefault()) {
            addresses.clearOtherDefaults(user.getId(), id);
        }

        mapper.update(address, request);
        addresses.saveAndFlush(address);

        return mapper.toResponse(address);
    }

    @Override
    @Transactional
    public void delete(String email, Long id) {
        User user = currentUser(email, true);
        Address address = ownedAddress(id, user.getId());

        addresses.delete(address);
    }

    @Override
    @Transactional
    public AddressResponse setDefault(String email, Long id) {
        User user = currentUser(email, true);
        Address address = ownedAddress(id, user.getId());

        addresses.clearOtherDefaults(user.getId(), id);

        address.setDefault(true);
        addresses.saveAndFlush(address);

        return mapper.toResponse(address);
    }

    private Address ownedAddress(Long id, Long userId) {
        return addresses.findByIdAndUser_Id(id, userId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Address not found"));
    }

    private User currentUser(String email, boolean lock) {
        User user = (lock
                ? users.findByEmailForUpdate(email)
                : users.findByEmail(email))
                .orElseThrow(() ->
                        new UnauthorizedException(
                                "User account is unavailable"));

        if (!user.isEnabled()) {
            throw new UnauthorizedException(
                    "User account is disabled");
        }

        return user;
    }
}