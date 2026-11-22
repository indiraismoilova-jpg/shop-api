package com.example.shop.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
public class ShippingAddress {

    @Column(name = "shipping_country", nullable = false, length = 100)
    private String country;

    @Column(name = "shipping_city", nullable = false, length = 100)
    private String city;

    @Column(name = "shipping_street", nullable = false, length = 200)
    private String street;

    @Column(name = "shipping_building", nullable = false, length = 30)
    private String building;

    @Column(name = "shipping_apartment", length = 30)
    private String apartment;

    @Column(name = "shipping_postal_code", length = 20)
    private String postalCode;

    @Column(name = "shipping_recipient_name", nullable = false, length = 200)
    private String recipientName;

    @Column(name = "shipping_recipient_phone", nullable = false, length = 30)
    private String recipientPhone;
}
