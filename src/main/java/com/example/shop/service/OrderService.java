package com.example.shop.service;

import com.example.shop.dto.request.*;
import com.example.shop.dto.response.*;
import com.example.shop.exception.*;
import com.example.shop.model.entity.*;
import com.example.shop.model.enums.*;
import com.example.shop.repository.CartRepository;
import com.example.shop.repository.OrderRepository;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import static com.example.shop.service.CommerceSupport.bad;
import static com.example.shop.service.CommerceSupport.conflict;

@Service
@Transactional
public class OrderService {

    private final OrderRepository orders;
    private final CartRepository cartRepository;
    private final CartService carts;
    private final DeliveryService delivery;
    private final PromoCodeService promos;
    private final CommerceSupport support;
    private final InventoryService inventory;
    public OrderService(
            OrderRepository orders,
            CartRepository cartRepository,
            CartService carts,
            DeliveryService delivery,
            PromoCodeService promos,
            CommerceSupport support,
            InventoryService inventory) {

        this.orders = orders;
        this.cartRepository = cartRepository;
        this.carts = carts;
        this.delivery = delivery;
        this.promos = promos;
        this.support = support;
        this.inventory = inventory;
    }

    @Transactional(readOnly = true)
    public Page<OrderResponse> list(
            String email,
            Pageable pageable) {

        User user = support.user(email, false);

        return orders.findByUserId(
                        user.getId(),
                        support.page(
                                pageable,
                                "id",
                                "createdAt",
                                "totalAmount",
                                "status"))
                .map(order -> response(order, false));
    }

    @Transactional(readOnly = true)
    public OrderResponse get(
            String email,
            String identifier) {

        return response(owned(email, identifier), false);
    }

    public OrderResponse create(
            String email,
            CreateOrderRequest request) {

        User user = support.user(email, true);
        Cart cart = carts.cart(user);

        if (cart.getCartItems().isEmpty()) {
            throw bad("Cart is empty");
        }

        Address address = support.find(
                Address.class,
                request.addressId());

        if (!address.getUser().getId().equals(user.getId())) {
            throw new ForbiddenException(
                    "Address belongs to another user");
        }

        DeliveryOption option = delivery.option(
                request.deliveryOptionId());

        DeliveryMethod method;

        try {
            method = DeliveryMethod.valueOf(
                    option.getCode().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw bad(
                    "Delivery option code must match a supported delivery method");
        }

        Order order = new Order();
        order.setUser(user);
        order.setOrderNumber(
                "ORD-" + UUID.randomUUID()
                        .toString()
                        .replace("-", "")
                        .toUpperCase(Locale.ROOT));

        order.setDeliveryMethod(method);
        order.setCustomerNote(request.customerNote());

        ShippingAddress shipping = new ShippingAddress();
        shipping.setCountry(address.getCountry());
        shipping.setCity(address.getCity());
        shipping.setStreet(address.getStreet());
        shipping.setBuilding(address.getBuilding());
        shipping.setApartment(address.getApartment());
        shipping.setPostalCode(address.getPostalCode());
        shipping.setRecipientName(address.getRecipientName());
        shipping.setRecipientPhone(address.getRecipientPhone());

        order.setShippingAddress(shipping);

        List<CartItem> sorted = cart.getCartItems()
                .stream()
                .sorted(Comparator
                        .comparing((CartItem item) ->
                                item.getProduct().getId())
                        .thenComparing(item ->
                                item.getVariant() == null
                                        ? 0L
                                        : item.getVariant().getId()))
                .toList();

        BigDecimal subtotal = BigDecimal.ZERO;

        for (CartItem item : sorted) {
            Product product = support.lock(
                    Product.class,
                    item.getProduct().getId());

            if (!product.isActive()) {
                throw bad("Product is inactive");
            }

            ProductVariant variant = item.getVariant() == null
                    ? null
                    : support.lock(
                    ProductVariant.class,
                    item.getVariant().getId());

            if (variant != null
                    && (!variant.isActive()
                    || !variant.getProduct().getId()
                    .equals(product.getId()))) {

                throw bad("Variant is inactive or invalid");
            }

            support.stock(
                    product,
                    variant,
                    item.getQuantity());



            BigDecimal price = support.price(product, variant);

            subtotal = subtotal.add(
                    price.multiply(
                            BigDecimal.valueOf(item.getQuantity())));

            OrderItem line = new OrderItem();
            line.setOrder(order);
            line.setProduct(product);
            line.setVariant(variant);
            line.setQuantity(item.getQuantity());
            line.setPriceAtPurchase(price);

            Map<String, Object> snapshot = new HashMap<>();
            snapshot.put("name", product.getName());
            snapshot.put(
                    "sku",
                    variant == null
                            ? product.getSku()
                            : variant.getSku());

            if (variant != null) {
                snapshot.put("variant", variant.getName());
            }

            line.setProductSnapshot(snapshot);
            order.getOrderItems().add(line);
        }

        BigDecimal shippingCost = delivery.price(
                option,
                subtotal);

        BigDecimal discount = BigDecimal.ZERO;

        if (cart.getAppliedPromoCode() != null) {
            PromoValidationResponse quote = promos.calculate(
                    user,
                    cart,
                    cart.getAppliedPromoCode(),
                    shippingCost,
                    true);

            discount = quote.discount()
                    .add(quote.shippingDiscount());

            order.setPromoCode(quote.code());
        }

        order.setSubtotal(subtotal);
        order.setShippingCost(shippingCost);
        order.setDiscount(discount);

        order.setTotalAmount(
                subtotal.add(shippingCost)
                        .subtract(discount)
                        .max(BigDecimal.ZERO));

        order.setEstimatedDeliveryDate(
                LocalDateTime.now()
                        .plusDays(option.getEstimatedDaysMax()));

        inventory.reserve(order);
        orders.saveAndFlush(order);

        if (order.getPromoCode() != null) {
            promos.consume(user, order);
        }

        cart.getCartItems().clear();
        cart.setAppliedPromoCode(null);
        cart.setUpdatedAt(LocalDateTime.now());

        cartRepository.flush();

        return response(order, false);
    }

    public OrderResponse cancel(
            String email,
            Long id) {

        Order order = lockedForUser(email, id);

        cancelOrder(order);

        return response(order, false);
    }

    public CartResponse reorder(
            String email,
            Long id,
            Pageable pageable) {

        User user = support.user(email, true);
        Order order = owned(email, id.toString());
        Cart cart = carts.cart(user);

        for (OrderItem item : order.getOrderItems()) {
            carts.addItem(
                    cart,
                    new CartItemRequest(
                            item.getProduct().getId(),
                            item.getVariant() == null
                                    ? null
                                    : item.getVariant().getId(),
                            item.getQuantity()));
        }

        cartRepository.flush();

        return carts.response(cart, pageable);
    }

    @Transactional(readOnly = true)
    public Page<OrderResponse> adminList(
            OrderStatus status,
            Long userId,
            String orderNumber,
            LocalDateTime from,
            LocalDateTime to,
            Pageable pageable) {

        if (from != null && to != null && from.isAfter(to)) {
            throw bad("Invalid date range");
        }

        return orders.findAll(
                        (root, query, builder) -> {
                            List<Predicate> terms = new ArrayList<>();

                            if (status != null) {
                                terms.add(builder.equal(
                                        root.get("status"),
                                        status));
                            }

                            if (userId != null) {
                                terms.add(builder.equal(
                                        root.get("user").get("id"),
                                        userId));
                            }

                            if (orderNumber != null) {
                                terms.add(builder.equal(
                                        root.get("orderNumber"),
                                        orderNumber));
                            }

                            if (from != null) {
                                terms.add(
                                        builder.greaterThanOrEqualTo(
                                                root.get("createdAt"),
                                                from));
                            }

                            if (to != null) {
                                terms.add(
                                        builder.lessThanOrEqualTo(
                                                root.get("createdAt"),
                                                to));
                            }

                            return builder.and(
                                    terms.toArray(Predicate[]::new));
                        },
                        support.page(
                                pageable,
                                "id",
                                "createdAt",
                                "totalAmount",
                                "status"))
                .map(order -> response(order, true));
    }

    public OrderResponse status(
            Long id,
            OrderStatus next) {

        Order order = adminLock(id);
        OrderStatus current = order.getStatus();

        if (!OrderStatusRules.canTransition(current, next)) {
            throw conflict(
                    "Invalid order status transition: "
                            + current + " -> " + next);
        }

        if (next == OrderStatus.CANCELLED) {
            cancelOrder(order);
            return response(order, true);
        }

        if (current == OrderStatus.DELIVERED
                && next == OrderStatus.REFUNDED) {

            throw conflict(
                    "A delivered order can only be refunded through a return");
        }

        Payment payment = order.getPayment();

        if (next == OrderStatus.REFUNDED) {
            if (payment == null
                    || payment.getStatus() != PaymentStatus.COMPLETED) {

                throw conflict(
                        "Only a completed payment can be refunded");
            }

            if (!Boolean.TRUE.equals(
                    payment.getMetadata().get("simulated"))) {

                throw conflict(
                        "A live provider refund is required");
            }

            inventory.restore(order);

            for (OrderItem item : sortedItems(order)) {
                Product product = support.lock(
                        Product.class,
                        item.getProduct().getId());

                product.setSalesCount(
                        Math.max(
                                0L,
                                product.getSalesCount()
                                        - item.getQuantity()));
            }

            payment.setStatus(PaymentStatus.REFUNDED);
            payment.setRefundedAmount(payment.getAmount());

            promos.release(order);
            order.setStatus(OrderStatus.REFUNDED);

            return response(order, true);
        }

        if (next == OrderStatus.PAID) {
            if (payment == null) {
                throw conflict("Payment not found");
            }

            if (payment.getPaymentMethod()
                    == PaymentMethod.CASH_ON_DELIVERY
                    && payment.getStatus() == PaymentStatus.PENDING) {

                payment.setStatus(PaymentStatus.COMPLETED);
                payment.setPaidAt(LocalDateTime.now());
                payment.setErrorCode(null);
                payment.setErrorMessage(null);
            }

            if (payment.getStatus() != PaymentStatus.COMPLETED) {
                throw conflict(
                        "Order must have a completed payment");
            }

            paid(order);

            return response(order, true);
        }

        boolean fulfillment = Set.of(
                        OrderStatus.PROCESSING,
                        OrderStatus.SHIPPED,
                        OrderStatus.IN_TRANSIT,
                        OrderStatus.DELIVERED)
                .contains(next);

        if (fulfillment
                && (payment == null
                || payment.getStatus() != PaymentStatus.COMPLETED)) {

            throw conflict(
                    "A completed payment is required before fulfillment");
        }

        if (next == OrderStatus.SHIPPED) {
            order.setShippedAt(LocalDateTime.now());

            if (order.getDeliveryTrackingNumber() == null) {
                order.setDeliveryTrackingNumber(
                        delivery.createTrackingNumber());
            }
        }

        if (next == OrderStatus.DELIVERED) {
            LocalDateTime now = LocalDateTime.now();

            order.setDeliveredAt(now);
            order.setActualDeliveryDate(now);
        }

        order.setStatus(next);

        return response(order, true);
    }

    public OrderResponse tracking(
            Long id,
            TrackingRequest request) {

        Order order = adminLock(id);
        order.setDeliveryTrackingNumber(request.trackingNumber());
        order.setEstimatedDeliveryDate(
                request.estimatedDeliveryDate());

        return response(order, true);
    }

    public OrderResponse note(
            Long id,
            OrderNoteRequest request) {

        Order order = adminLock(id);
        order.setAdminNote(request.note());

        return response(order, true);
    }

    public Order lockedForUser(
            String email,
            Long id) {

        User user = support.user(email, true);
        Order order = support.lock(Order.class, id);

        if (!order.getUser().getId().equals(user.getId())) {
            throw new ForbiddenException(
                    "Order belongs to another user");
        }

        return order;
    }

    @Transactional(readOnly = true)
    public Order owned(
            String email,
            String identifier) {

        User user = support.user(email, false);
        Order order;

        if (identifier.matches("[0-9]+")) {
            try {
                order = support.find(
                        Order.class,
                        Long.valueOf(identifier));

            } catch (NumberFormatException exception) {
                throw bad("Invalid order id");
            }
        } else {
            order = orders.findByOrderNumber(identifier)
                    .orElseThrow(() ->
                            new ResourceNotFoundException(
                                    "Order not found"));
        }

        if (!order.getUser().getId().equals(user.getId())) {
            throw new ForbiddenException(
                    "Order belongs to another user");
        }

        return order;
    }

    public void paid(Order order) {
        if (!OrderStatusRules.canTransition(
                order.getStatus(),
                OrderStatus.PAID)) {

            throw conflict(
                    "Only a confirmed order can be paid");
        }

        inventory.commit(order);

        order.setPaidAt(LocalDateTime.now());
        order.setStatus(OrderStatus.PAID);

        incrementSales(order);
    }

    private Order adminLock(Long id) {
        Order order = support.find(Order.class, id);

        support.user(order.getUser().getEmail(), true);

        return support.lock(Order.class, id);
    }

    private void incrementSales(Order order) {
        for (OrderItem item : sortedItems(order)) {
            Product product = support.lock(
                    Product.class,
                    item.getProduct().getId());

            product.setSalesCount(
                    product.getSalesCount() + item.getQuantity());
        }
    }

    private List<OrderItem> sortedItems(Order order) {
        return order.getOrderItems()
                .stream()
                .sorted(Comparator
                        .comparing((OrderItem item) ->
                                item.getProduct().getId())
                        .thenComparing(item ->
                                item.getVariant() == null
                                        ? 0L
                                        : item.getVariant().getId()))
                .toList();
    }

    private void cancelOrder(Order order) {
        if (!OrderStatusRules.canTransition(
                order.getStatus(),
                OrderStatus.CANCELLED)) {

            throw new OrderCancellationException(
                    "This order can no longer be cancelled");
        }

        Payment payment = order.getPayment();

        boolean completed = payment != null
                && payment.getStatus() == PaymentStatus.COMPLETED;

        if (completed
                && !Boolean.TRUE.equals(
                payment.getMetadata().get("simulated"))) {

            throw conflict(
                    "A provider refund is required before cancellation");
        }

        if (completed) {
            inventory.restore(order);

            for (OrderItem item : sortedItems(order)) {
                Product product = support.lock(
                        Product.class,
                        item.getProduct().getId());

                product.setSalesCount(
                        Math.max(
                                0L,
                                product.getSalesCount()
                                        - item.getQuantity()));
            }
        } else {
            inventory.release(order);
        }

        if (payment != null) {
            if (completed) {
                payment.setStatus(PaymentStatus.REFUNDED);
                payment.setRefundedAmount(payment.getAmount());
            } else {
                payment.setStatus(PaymentStatus.FAILED);
                payment.setErrorCode("ORDER_CANCELLED");
                payment.setErrorMessage("Order was cancelled");
            }
        }

        promos.release(order);
        order.setStatus(OrderStatus.CANCELLED);
    }
    public OrderResponse response(
            Order order,
            boolean admin) {

        ShippingAddress address = order.getShippingAddress();

        ShippingAddressResponse shipping =
                new ShippingAddressResponse(
                        address.getCountry(),
                        address.getCity(),
                        address.getStreet(),
                        address.getBuilding(),
                        address.getApartment(),
                        address.getPostalCode(),
                        address.getRecipientName(),
                        address.getRecipientPhone());

        List<OrderItemResponse> rows = order.getOrderItems()
                .stream()
                .map(item -> new OrderItemResponse(
                        item.getId(),
                        item.getProduct().getId(),
                        item.getVariant() == null
                                ? null
                                : item.getVariant().getId(),
                        String.valueOf(
                                item.getProductSnapshot().get("name")),
                        item.getQuantity(),
                        item.getPriceAtPurchase(),
                        item.getPriceAtPurchase().multiply(
                                BigDecimal.valueOf(item.getQuantity()))))
                .toList();

        return new OrderResponse(
                order.getId(),
                order.getOrderNumber(),
                order.getStatus(),
                shipping,
                order.getDeliveryMethod(),
                order.getSubtotal(),
                order.getShippingCost(),
                order.getDiscount(),
                order.getTotalAmount(),
                order.getPromoCode(),
                order.getCustomerNote(),
                admin ? order.getAdminNote() : null,
                order.getDeliveryTrackingNumber(),
                order.getEstimatedDeliveryDate(),
                order.getCreatedAt(),
                order.getPaidAt(),
                order.getShippedAt(),
                order.getDeliveredAt(),
                new PageImpl<>(
                        rows,
                        PageRequest.of(0, 100),
                        rows.size()));
    }
}
