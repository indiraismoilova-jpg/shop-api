package com.example.shop.service;

import com.example.shop.dto.request.PromoCodeRequest;
import com.example.shop.dto.response.PromoResponse;
import com.example.shop.dto.response.PromoValidationResponse;
import com.example.shop.exception.PromoCodeInvalidException;
import com.example.shop.model.entity.*;
import com.example.shop.model.enums.DiscountType;
import com.example.shop.repository.CartRepository;
import com.example.shop.repository.PromoCodeRepository;
import com.example.shop.repository.PromoCodeUsageRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

import static com.example.shop.service.CommerceSupport.bad;
import static com.example.shop.service.CommerceSupport.conflict;

@Service
@Transactional(readOnly = true)
public class PromoCodeService {

    private final PromoCodeRepository promos;
    private final PromoCodeUsageRepository usages;
    private final CartRepository carts;
    private final CommerceSupport support;

    public PromoCodeService(
            PromoCodeRepository promos,
            PromoCodeUsageRepository usages,
            CartRepository carts,
            CommerceSupport support) {

        this.promos = promos;
        this.usages = usages;
        this.carts = carts;
        this.support = support;
    }

    public Page<PromoResponse> list(Pageable pageable) {
        return promos.findAll(
                        support.page(
                                pageable,
                                "id",
                                "code",
                                "validFrom",
                                "validTo"))
                .map(this::response);
    }

    @Transactional
    public PromoResponse save(
            Long id,
            String email,
            PromoCodeRequest request) {

        User user = support.user(email, false);
        String code = request.code().toUpperCase(Locale.ROOT);

        if (promos.existsByCodeIgnoreCaseAndIdNot(
                code,
                id == null ? -1L : id)) {

            throw conflict("Promo code already exists");
        }

        if (!request.validTo().isAfter(request.validFrom())) {
            throw bad("validTo must be after validFrom");
        }

        if (request.discountType() == DiscountType.PERCENTAGE
                && request.discountValue()
                .compareTo(BigDecimal.valueOf(100)) > 0) {

            throw bad("Percentage must not exceed 100");
        }

        PromoCode promo = id == null
                ? new PromoCode()
                : support.lock(PromoCode.class, id);

        if (request.maxUsageCount() != null
                && request.maxUsageCount()
                < promo.getCurrentUsageCount()) {

            throw bad("Usage limit is below current usage");
        }

        promo.setCode(code);
        promo.setDescription(request.description());
        promo.setDiscountType(request.discountType());
        promo.setDiscountValue(request.discountValue());
        promo.setMinOrderAmount(request.minOrderAmount());
        promo.setMaxDiscountAmount(request.maxDiscountAmount());
        promo.setMaxUsageCount(request.maxUsageCount());
        promo.setMaxUsagePerUser(request.maxUsagePerUser());
        promo.setValidFrom(request.validFrom());
        promo.setValidTo(request.validTo());
        promo.setActive(request.isActive());

        if (id == null) {
            promo.setCreatedBy(user);
        }

        Set<Category> categories = new HashSet<>();

        if (request.categoryIds() != null) {
            for (Long categoryId : request.categoryIds()) {
                categories.add(
                        support.find(Category.class, categoryId));
            }
        }

        promo.setApplicableCategories(categories);

        Set<Product> products = new HashSet<>();

        if (request.productIds() != null) {
            for (Long productId : request.productIds()) {
                products.add(
                        support.find(Product.class, productId));
            }
        }

        promo.setApplicableProducts(products);

        return response(promos.saveAndFlush(promo));
    }

    @Transactional
    public void delete(Long id) {
        support.lock(PromoCode.class, id).setActive(false);
    }

    public PromoValidationResponse validate(
            String email,
            String code) {

        User user = support.user(email, false);

        Cart cart = carts
                .findFirstByUserIdOrderByIdAsc(user.getId())
                .orElseThrow(() ->
                        new PromoCodeInvalidException("Cart is empty"));

        return calculate(
                user,
                cart,
                code,
                BigDecimal.ZERO,
                false);
    }

    public PromoValidationResponse calculate(
            User user,
            Cart cart,
            String code,
            BigDecimal shipping,
            boolean lock) {

        PromoCode promo = promos.findByCodeIgnoreCase(code)
                .orElseThrow(() ->
                        new PromoCodeInvalidException(
                                "Promo code not found"));

        if (lock) {
            promo = support.lock(
                    PromoCode.class,
                    promo.getId());
        }

        LocalDateTime now = LocalDateTime.now();

        if (!promo.isActive()
                || now.isBefore(promo.getValidFrom())
                || !now.isBefore(promo.getValidTo())) {

            throw new PromoCodeInvalidException(
                    "Promo code is inactive or outside its validity period");
        }

        if (promo.getMaxUsageCount() != null
                && promo.getCurrentUsageCount()
                >= promo.getMaxUsageCount()) {

            throw new PromoCodeInvalidException(
                    "Promo usage limit reached");
        }

        if (promo.getMaxUsagePerUser() != null
                && usages.countByPromoCodeIdAndUserId(
                promo.getId(),
                user.getId())
                >= promo.getMaxUsagePerUser()) {

            throw new PromoCodeInvalidException(
                    "Your promo usage limit is reached");
        }

        Set<Long> productIds = promo.getApplicableProducts()
                .stream()
                .map(Product::getId)
                .collect(Collectors.toSet());

        Set<Long> categoryIds = promo.getApplicableCategories()
                .stream()
                .map(Category::getId)
                .collect(Collectors.toSet());

        BigDecimal subtotal = BigDecimal.ZERO;
        BigDecimal eligible = BigDecimal.ZERO;

        for (CartItem item : cart.getCartItems()) {
            BigDecimal line = support.price(
                            item.getProduct(),
                            item.getVariant())
                    .multiply(
                            BigDecimal.valueOf(item.getQuantity()));

            subtotal = subtotal.add(line);

            boolean unrestricted =
                    productIds.isEmpty() && categoryIds.isEmpty();

            boolean productAllowed = productIds.contains(
                    item.getProduct().getId());

            boolean categoryAllowed = categoryIds.contains(
                    item.getProduct().getCategory().getId());

            if (unrestricted || productAllowed || categoryAllowed) {
                eligible = eligible.add(line);
            }
        }

        if (eligible.signum() <= 0) {
            throw new PromoCodeInvalidException(
                    "No eligible products in cart");
        }

        if (promo.getMinOrderAmount() != null
                && subtotal.compareTo(promo.getMinOrderAmount()) < 0) {

            throw new PromoCodeInvalidException(
                    "Minimum order amount not reached");
        }

        BigDecimal discount = switch (promo.getDiscountType()) {
            case PERCENTAGE -> eligible
                    .multiply(promo.getDiscountValue())
                    .divide(
                            BigDecimal.valueOf(100),
                            2,
                            RoundingMode.HALF_UP);

            case FIXED_AMOUNT ->
                    promo.getDiscountValue().min(eligible);

            case FREE_SHIPPING -> BigDecimal.ZERO;
        };

        BigDecimal shippingDiscount =
                promo.getDiscountType() == DiscountType.FREE_SHIPPING
                        ? shipping
                        : BigDecimal.ZERO;

        if (promo.getMaxDiscountAmount() != null) {
            discount = discount.min(
                    promo.getMaxDiscountAmount());

            shippingDiscount = shippingDiscount.min(
                    promo.getMaxDiscountAmount());
        }

        return new PromoValidationResponse(
                promo.getCode(),
                true,
                discount.min(eligible),
                shippingDiscount);
    }

    @Transactional
    public void consume(User user, Order order) {
        PromoCode promo = promos
                .findByCodeIgnoreCase(order.getPromoCode())
                .orElseThrow();

        promo = support.lock(
                PromoCode.class,
                promo.getId());

        promo.setCurrentUsageCount(
                promo.getCurrentUsageCount() + 1);

        PromoCodeUsage usage = new PromoCodeUsage();
        usage.setPromoCode(promo);
        usage.setUser(user);
        usage.setOrder(order);
        usage.setDiscountApplied(order.getDiscount());
        usage.setUsedAt(LocalDateTime.now());

        usages.save(usage);
    }

    @Transactional
    public void release(Order order) {
        for (PromoCodeUsage usage
                : usages.findByOrderId(order.getId())) {

            PromoCode promo = support.lock(
                    PromoCode.class,
                    usage.getPromoCode().getId());

            promo.setCurrentUsageCount(
                    Math.max(
                            0,
                            promo.getCurrentUsageCount() - 1));

            usages.delete(usage);
        }
    }

    private PromoResponse response(PromoCode promo) {
        Set<Long> categoryIds = promo.getApplicableCategories()
                .stream()
                .map(Category::getId)
                .collect(Collectors.toSet());

        Set<Long> productIds = promo.getApplicableProducts()
                .stream()
                .map(Product::getId)
                .collect(Collectors.toSet());

        return new PromoResponse(
                promo.getId(),
                promo.getCode(),
                promo.getDescription(),
                promo.getDiscountType(),
                promo.getDiscountValue(),
                promo.getMinOrderAmount(),
                promo.getMaxDiscountAmount(),
                promo.getMaxUsageCount(),
                promo.getCurrentUsageCount(),
                promo.getMaxUsagePerUser(),
                promo.getValidFrom(),
                promo.getValidTo(),
                promo.isActive(),
                categoryIds,
                productIds);
    }
}