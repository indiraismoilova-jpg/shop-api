package com.example.shop.service;

import com.example.shop.dto.response.DeliveryQuoteResponse;
import com.example.shop.dto.response.DeliveryResponse;
import com.example.shop.exception.ResourceNotFoundException;
import com.example.shop.model.entity.DeliveryOption;
import com.example.shop.repository.DeliveryOptionRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.math.BigDecimal;
import java.util.Locale;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class DeliveryService {

    private final DeliveryOptionRepository options;
    private final CommerceSupport support;

    public DeliveryService(
            DeliveryOptionRepository options,
            CommerceSupport support) {

        this.options = options;
        this.support = support;
    }

    public Page<DeliveryResponse> methods(Pageable pageable) {
        return options.findByIsActiveTrue(
                        support.page(
                                pageable,
                                "id",
                                "name",
                                "basePrice"))
                .map(option -> new DeliveryResponse(
                        option.getId(),
                        option.getCode(),
                        option.getName(),
                        option.getDescription(),
                        option.getBasePrice(),
                        option.getFreeThreshold(),
                        option.getEstimatedDaysMin(),
                        option.getEstimatedDaysMax()));
    }

    public DeliveryOption option(Long id) {
        DeliveryOption option = support.find(
                DeliveryOption.class,
                id);

        if (!option.isActive()) {
            throw new ResourceNotFoundException(
                    "Delivery option not found");
        }

        return option;
    }

    public DeliveryQuoteResponse calculate(
            Long id,
            BigDecimal subtotal) {

        DeliveryOption option = option(id);
        LocalDate today = LocalDate.now();

        return new DeliveryQuoteResponse(
                id,
                price(option, subtotal),
                option.getEstimatedDaysMin(),
                option.getEstimatedDaysMax(),
                today.plusDays(option.getEstimatedDaysMin()),
                today.plusDays(option.getEstimatedDaysMax()));
    }

    public BigDecimal price(
            DeliveryOption option,
            BigDecimal subtotal) {

        if (option.getFreeThreshold() != null
                && subtotal.compareTo(
                option.getFreeThreshold()) >= 0) {

            return BigDecimal.ZERO;
        }

        return option.getBasePrice();
    }

    public String createTrackingNumber() {
        return "TRK-" + UUID.randomUUID()
                .toString()
                .replace("-", "")
                .toUpperCase(Locale.ROOT);
    }
}