package com.example.shop.exception;

public class PromoCodeInvalidException extends RuntimeException {

    public PromoCodeInvalidException(String message) {
        super(message);
    }
}