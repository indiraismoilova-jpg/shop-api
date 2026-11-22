package com.example.shop.mapper;

import com.example.shop.dto.request.CreateAddressRequest;
import com.example.shop.dto.request.UpdateAddressRequest;
import com.example.shop.dto.response.AddressResponse;
import com.example.shop.model.entity.Address;
import org.springframework.stereotype.Component;

@Component
public class AddressMapper {

    public Address toEntity(CreateAddressRequest request) {
        Address address = new Address();

        address.setTitle(request.title());
        address.setCountry(request.country());
        address.setCity(request.city());
        address.setStreet(request.street());
        address.setBuilding(request.building());
        address.setApartment(request.apartment());
        address.setPostalCode(request.postalCode());
        address.setRecipientName(request.recipientName());
        address.setRecipientPhone(request.recipientPhone());
        address.setDefault(request.isDefault());
        address.setLatitude(request.latitude());
        address.setLongitude(request.longitude());

        return address;
    }

    public void update(
            Address address,
            UpdateAddressRequest request) {

        address.setTitle(request.title());
        address.setCountry(request.country());
        address.setCity(request.city());
        address.setStreet(request.street());
        address.setBuilding(request.building());
        address.setApartment(request.apartment());
        address.setPostalCode(request.postalCode());
        address.setRecipientName(request.recipientName());
        address.setRecipientPhone(request.recipientPhone());
        address.setDefault(request.isDefault());
        address.setLatitude(request.latitude());
        address.setLongitude(request.longitude());
    }

    public AddressResponse toResponse(Address address) {
        return new AddressResponse(
                address.getId(),
                address.getTitle(),
                address.getCountry(),
                address.getCity(),
                address.getStreet(),
                address.getBuilding(),
                address.getApartment(),
                address.getPostalCode(),
                address.getRecipientName(),
                address.getRecipientPhone(),
                address.isDefault(),
                address.getLatitude(),
                address.getLongitude());
    }
}