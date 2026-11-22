package com.example.shop.service;

import com.example.shop.dto.request.PaymentRequest;
import com.example.shop.dto.response.PaymentMethodResponse;
import com.example.shop.dto.response.PaymentResponse;
import com.example.shop.exception.PaymentFailedException;
import com.example.shop.exception.ResourceNotFoundException;
import com.example.shop.model.entity.Order;
import com.example.shop.model.entity.Payment;
import com.example.shop.model.enums.OrderStatus;
import com.example.shop.model.enums.PaymentMethod;
import com.example.shop.model.enums.PaymentStatus;
import com.example.shop.repository.PaymentRepository;
import com.example.shop.validator.LuhnValidator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static com.example.shop.service.CommerceSupport.bad;
import static com.example.shop.service.CommerceSupport.conflict;

@Service
@Transactional
public class PaymentService {

    private final PaymentRepository payments;
    private final OrderService orders;
    private final CommerceSupport support;
    private final SimulatedPaymentGateway gateway;
    private final boolean simulation;
    private final String currency;

    public PaymentService(
            PaymentRepository payments,
            OrderService orders,
            CommerceSupport support,
            SimulatedPaymentGateway gateway,
            @Value("${payments.simulation.enabled:true}")
            boolean simulation,
            @Value("${shop.currency:USD}")
            String currency) {

        this.payments = payments;
        this.orders = orders;
        this.support = support;
        this.gateway = gateway;
        this.simulation = simulation;
        this.currency = currency;
    }

    @Transactional(noRollbackFor = PaymentFailedException.class)
    public PaymentResponse process(
            String email,
            PaymentRequest request) {

        if (request.paymentMethod() == PaymentMethod.CARD
                && !LuhnValidator.isValid(request.cardNumber())) {

            throw bad("Invalid card number: Luhn check failed");
        }

        if (request.paymentMethod() == PaymentMethod.BONUS_POINTS) {
            throw bad(
                    "Bonus points belong to the optional bonus module");
        }

        if (!simulation
                && request.paymentMethod()
                != PaymentMethod.CASH_ON_DELIVERY) {

            throw new PaymentFailedException(
                    "No live payment provider is configured");
        }

        Order order = orders.lockedForUser(
                email,
                request.orderId());

        Payment payment = payments.findByOrderId(order.getId())
                .orElse(null);

        if (payment != null
                && payment.getStatus() == PaymentStatus.COMPLETED) {

            if (order.getStatus() == OrderStatus.CANCELLED
                    || order.getStatus() == OrderStatus.REFUNDED) {

                throw conflict("This order cannot be paid");
            }

            if (payment.getPaymentMethod()
                    != request.paymentMethod()) {

                throw conflict(
                        "Order was already paid using another method");
            }

            return response(payment);
        }

        if (order.getStatus() == OrderStatus.PENDING) {
            if (!OrderStatusRules.canTransition(
                    order.getStatus(),
                    OrderStatus.CONFIRMED)) {

                throw conflict("Order cannot be confirmed");
            }

            order.setStatus(OrderStatus.CONFIRMED);
        }

        if (order.getStatus() != OrderStatus.CONFIRMED) {
            throw conflict(
                    "Only a confirmed order can be paid");
        }

        if (payment == null) {
            payment = new Payment();
            payment.setOrder(order);
        }

        payment.setPaymentMethod(request.paymentMethod());
        payment.setAmount(order.getTotalAmount());
        payment.setCurrency(currency);
        payment.setPaidAt(null);
        payment.setErrorCode(null);
        payment.setErrorMessage(null);
        payment.setCardLastFourDigits(null);
        payment.getMetadata().put("simulated", simulation);

        if (request.paymentMethod()
                == PaymentMethod.CASH_ON_DELIVERY) {

            payment.setStatus(PaymentStatus.PENDING);
            payment.setTransactionId("COD-" + UUID.randomUUID());

            order.setPayment(payment);
            payments.saveAndFlush(payment);

            return response(payment);
        }

        GatewayResult result;

        if (request.paymentMethod() == PaymentMethod.CARD) {
            result = gateway.charge(request.cardNumber());
        } else {
            result = new GatewayResult(
                    true,
                    "SIM-PAYPAL-" + UUID.randomUUID(),
                    null,
                    null,
                    null);
        }

        payment.setTransactionId(result.transactionId());
        payment.setCardLastFourDigits(result.lastFourDigits());

        order.setPayment(payment);

        if (!result.approved()) {
            payment.setStatus(PaymentStatus.FAILED);
            payment.setErrorCode(result.errorCode());
            payment.setErrorMessage(result.errorMessage());

            payments.saveAndFlush(payment);

            throw new PaymentFailedException(
                    result.errorMessage());
        }

        payment.setStatus(PaymentStatus.COMPLETED);
        payment.setPaidAt(LocalDateTime.now());

        orders.paid(order);

        payments.saveAndFlush(payment);

        return response(payment);
    }

    @Transactional(readOnly = true)
    public PaymentResponse status(
            String email,
            Long orderId) {

        Order order = orders.owned(
                email,
                orderId.toString());

        Payment payment = payments.findByOrderId(order.getId())
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Payment not found"));

        return response(payment);
    }

    @Transactional(readOnly = true)
    public Page<PaymentMethodResponse> methods(
            Pageable pageable) {

        Pageable page = support.page(pageable, "id");

        List<PaymentMethodResponse> rows = new ArrayList<>();

        if (simulation) {
            rows.add(new PaymentMethodResponse(
                    PaymentMethod.CARD,
                    "Card (simulation)",
                    true));

            rows.add(new PaymentMethodResponse(
                    PaymentMethod.PAYPAL,
                    "PayPal (simulation)",
                    true));
        }

        rows.add(new PaymentMethodResponse(
                PaymentMethod.CASH_ON_DELIVERY,
                "Cash on delivery",
                simulation));

        int start = (int) Math.min(
                page.getOffset(),
                rows.size());

        int end = Math.min(
                start + page.getPageSize(),
                rows.size());

        return new PageImpl<>(
                rows.subList(start, end),
                page,
                rows.size());
    }

    private PaymentResponse response(Payment payment) {
        return new PaymentResponse(
                payment.getId(),
                payment.getOrder().getId(),
                payment.getTransactionId(),
                payment.getPaymentMethod(),
                payment.getStatus(),
                payment.getAmount(),
                payment.getCurrency(),
                payment.getPaidAt(),
                Boolean.TRUE.equals(
                        payment.getMetadata().get("simulated")),
                payment.getCardLastFourDigits(),
                payment.getErrorCode(),
                payment.getErrorMessage());
    }
}