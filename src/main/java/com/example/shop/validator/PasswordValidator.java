package com.example.shop.validator;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;

public class PasswordValidator
        implements ConstraintValidator<ValidPassword, String> {

    private static final Pattern PATTERN = Pattern.compile(
            "^(?=.*[a-z])(?=.*[A-Z])(?=.*[0-9])"
                    + "(?=.*[^A-Za-z0-9\\s])\\S{8,72}$");

    @Override
    public boolean isValid(
            String password,
            ConstraintValidatorContext context) {

        if (password == null) {
            return true;
        }

        return password.getBytes(StandardCharsets.UTF_8).length <= 72
                && PATTERN.matcher(password).matches();
    }
}