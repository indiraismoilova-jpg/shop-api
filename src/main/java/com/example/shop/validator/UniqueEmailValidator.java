package com.example.shop.validator;

import com.example.shop.repository.UserRepository;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.Locale;

public class UniqueEmailValidator
        implements ConstraintValidator<UniqueEmail, String> {

    private final UserRepository users;

    public UniqueEmailValidator(UserRepository users) {
        this.users = users;
    }

    @Override
    public boolean isValid(
            String email,
            ConstraintValidatorContext context) {

        if (email == null || email.isBlank()) {
            return true;
        }

        String normalized = email.trim()
                .toLowerCase(Locale.ROOT);

        return !users.existsByEmail(normalized);
    }
}