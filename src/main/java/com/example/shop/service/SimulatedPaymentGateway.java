package com.example.shop.service;

import com.example.shop.validator.LuhnValidator;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import static com.example.shop.service.CommerceSupport.bad;

@Component
public class SimulatedPaymentGateway {

    public GatewayResult charge(String cardNumber) {
        if (!LuhnValidator.isValid(cardNumber)) {
            throw bad("Invalid card number: Luhn check failed");
        }

        String transactionId = "SIM-" + UUID.randomUUID();

        String lastFourDigits = cardNumber.substring(
                cardNumber.length() - 4);

        try {
            Thread.sleep(
                    ThreadLocalRandom.current()
                            .nextInt(1000, 3001));

        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();

            return new GatewayResult(
                    false,
                    transactionId,
                    lastFourDigits,
                    "INTERRUPTED",
                    "Payment processing was interrupted");
        }

        return switch (cardNumber) {
            case "4111111111111111" -> success(
                    transactionId,
                    lastFourDigits);

            case "4000000000000002" -> failure(
                    transactionId,
                    lastFourDigits,
                    "INSUFFICIENT_FUNDS",
                    "Payment declined: insufficient funds");

            case "4000000000000069" -> failure(
                    transactionId,
                    lastFourDigits,
                    "NETWORK_ERROR",
                    "Payment gateway network error");

            case "4000000000000077" -> failure(
                    transactionId,
                    lastFourDigits,
                    "SUSPECTED_FRAUD",
                    "Payment declined: suspected fraud");

            default -> randomResult(
                    transactionId,
                    lastFourDigits);
        };
    }

    private GatewayResult randomResult(
            String transactionId,
            String lastFourDigits) {

        if (ThreadLocalRandom.current().nextInt(10) == 0) {
            return failure(
                    transactionId,
                    lastFourDigits,
                    "RANDOM_FAILURE",
                    "Simulated payment failure");
        }

        return success(transactionId, lastFourDigits);
    }

    private GatewayResult success(
            String transactionId,
            String lastFourDigits) {

        return new GatewayResult(
                true,
                transactionId,
                lastFourDigits,
                null,
                null);
    }

    private GatewayResult failure(
            String transactionId,
            String lastFourDigits,
            String errorCode,
            String errorMessage) {

        return new GatewayResult(
                false,
                transactionId,
                lastFourDigits,
                errorCode,
                errorMessage);
    }
}