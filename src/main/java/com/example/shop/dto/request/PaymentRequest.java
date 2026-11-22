package com.example.shop.dto.request;

import com.example.shop.model.enums.PaymentMethod;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

public record PaymentRequest(
        @NotNull
        @Positive
        Long orderId,

        @NotNull
        PaymentMethod paymentMethod,

        @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
        @Pattern(
                regexp = "[0-9]{13,19}",
                message = "Card number must contain 13 to 19 digits")
        String cardNumber
) {

        @Override
        public String toString() {
                return "PaymentRequest[orderId=" + orderId
                        + ", paymentMethod=" + paymentMethod
                        + ", cardNumber=REDACTED]";
        }
}