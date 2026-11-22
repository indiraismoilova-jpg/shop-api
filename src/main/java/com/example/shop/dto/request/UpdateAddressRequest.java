package com.example.shop.dto.request;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record UpdateAddressRequest(

        @Size(max = 100)
        String title,

        @NotBlank
        @Size(max = 100)
        String country,

        @NotBlank
        @Size(max = 100)
        String city,

        @NotBlank
        @Size(max = 200)
        String street,

        @NotBlank
        @Size(max = 30)
        String building,

        @Size(max = 30)
        String apartment,

        @Size(max = 20)
        String postalCode,

        @NotBlank
        @Size(max = 200)
        String recipientName,

        @NotBlank
        @Size(max = 30)
        String recipientPhone,

        boolean isDefault,

        @DecimalMin("-90")
        @DecimalMax("90")
        @Digits(integer = 3, fraction = 7)
        BigDecimal latitude,

        @DecimalMin("-180")
        @DecimalMax("180")
        @Digits(integer = 3, fraction = 7)
        BigDecimal longitude
) {
}