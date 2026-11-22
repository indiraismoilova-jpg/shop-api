package com.example.shop.validator;

public final class LuhnValidator {

    private LuhnValidator() {
    }

    public static boolean isValid(String cardNumber) {
        if (cardNumber == null
                || !cardNumber.matches("[0-9]{13,19}")) {

            return false;
        }

        int sum = 0;
        boolean doubleDigit = false;
        boolean hasNonZeroDigit = false;

        for (int i = cardNumber.length() - 1; i >= 0; i--) {
            int digit = cardNumber.charAt(i) - '0';

            if (digit != 0) {
                hasNonZeroDigit = true;
            }

            if (doubleDigit) {
                digit *= 2;

                if (digit > 9) {
                    digit -= 9;
                }
            }

            sum += digit;
            doubleDigit = !doubleDigit;
        }

        return hasNonZeroDigit && sum % 10 == 0;
    }
}