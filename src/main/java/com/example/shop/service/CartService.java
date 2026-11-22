package com.example.shop.service;

import com.example.shop.dto.request.CartItemRequest;
import com.example.shop.dto.request.MergeCartRequest;
import com.example.shop.dto.request.QuantityRequest;
import com.example.shop.dto.response.CartItemResponse;
import com.example.shop.dto.response.CartResponse;
import com.example.shop.dto.response.PromoValidationResponse;
import com.example.shop.exception.PromoCodeInvalidException;
import com.example.shop.exception.ResourceNotFoundException;
import com.example.shop.model.entity.*;
import com.example.shop.repository.CartItemRepository;
import com.example.shop.repository.CartRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

import static com.example.shop.service.CommerceSupport.bad;

@Service
@Transactional
public class CartService {

    private final CartRepository carts;
    private final CartItemRepository items;
    private final PromoCodeService promos;
    private final CommerceSupport support;

    public CartService(
            CartRepository carts,
            CartItemRepository items,
            PromoCodeService promos,
            CommerceSupport support) {

        this.carts = carts;
        this.items = items;
        this.promos = promos;
        this.support = support;
    }

    public Cart cart(User user) {
        return carts.findFirstByUserIdOrderByIdAsc(user.getId())
                .orElseGet(() -> {
                    Cart cart = new Cart();
                    cart.setUser(user);

                    return carts.saveAndFlush(cart);
                });
    }

    public CartResponse get(
            String email,
            Pageable pageable) {

        User user = support.user(email, true);

        return response(cart(user), pageable);
    }

    public CartResponse add(
            String email,
            CartItemRequest request,
            Pageable pageable) {

        Cart cart = cart(support.user(email, true));

        addItem(cart, request);
        carts.flush();

        return response(cart, pageable);
    }

    public void addItem(
            Cart cart,
            CartItemRequest request) {

        Product product = support.activeProduct(
                request.productId());

        ProductVariant variant = support.variant(
                product,
                request.variantId());

        CartItem item = cart.getCartItems()
                .stream()
                .filter(existing ->
                        existing.getProduct().getId()
                                .equals(product.getId())
                                && Objects.equals(
                                existing.getVariant() == null
                                        ? null
                                        : existing.getVariant().getId(),
                                request.variantId()))
                .findFirst()
                .orElse(null);

        int quantity = request.quantity()
                + (item == null ? 0 : item.getQuantity());

        if (quantity > 10000) {
            throw bad("Maximum quantity is 10000");
        }

        support.stock(product, variant, quantity);

        if (item == null) {
            if (cart.getCartItems().size() >= 100) {
                throw bad("Maximum cart size is 100 lines");
            }

            item = new CartItem();
            item.setCart(cart);
            item.setProduct(product);
            item.setVariant(variant);
            item.setPriceAtAdd(
                    support.price(product, variant));

            cart.getCartItems().add(item);
        }

        item.setQuantity(quantity);

        cart.setUpdatedAt(LocalDateTime.now());
        carts.save(cart);
    }
    public CartResponse update(
            String email,
            Long itemId,
            QuantityRequest request,
            Pageable pageable) {

        Cart cart = cart(support.user(email, true));
        CartItem item = findItem(cart, itemId);

        Product product = support.activeProduct(
                item.getProduct().getId());

        ProductVariant variant = support.variant(
                product,
                item.getVariant() == null
                        ? null
                        : item.getVariant().getId());

        support.stock(
                product,
                variant,
                request.quantity());

        item.setQuantity(request.quantity());
        cart.setUpdatedAt(LocalDateTime.now());

        carts.flush();

        return response(cart, pageable);
    }

    public void remove(
            String email,
            Long itemId) {

        Cart cart = cart(support.user(email, true));

        cart.getCartItems().remove(findItem(cart, itemId));
        cart.setAppliedPromoCode(null);
        cart.setUpdatedAt(LocalDateTime.now());

        carts.flush();
    }

    public void clear(String email) {
        Cart cart = cart(support.user(email, true));

        cart.getCartItems().clear();
        cart.setAppliedPromoCode(null);
        cart.setUpdatedAt(LocalDateTime.now());

        carts.flush();
    }

    public CartResponse apply(
            String email,
            String code,
            Pageable pageable) {

        User user = support.user(email, true);
        Cart cart = cart(user);

        PromoValidationResponse result = promos.calculate(
                user,
                cart,
                code,
                BigDecimal.ZERO,
                false);

        cart.setAppliedPromoCode(result.code());

        return response(cart, pageable);
    }

    public CartResponse removePromo(
            String email,
            Pageable pageable) {

        Cart cart = cart(support.user(email, true));

        cart.setAppliedPromoCode(null);

        return response(cart, pageable);
    }

    public CartResponse merge(
            String email,
            MergeCartRequest request,
            Pageable pageable) {

        Cart cart = cart(support.user(email, true));

        for (CartItemRequest item : request.items()) {
            addItem(cart, item);
        }

        carts.flush();

        return response(cart, pageable);
    }

    public BigDecimal subtotal(Cart cart) {
        return cart.getCartItems()
                .stream()
                .map(item -> support.price(
                                item.getProduct(),
                                item.getVariant())
                        .multiply(
                                BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public CartResponse response(
            Cart cart,
            Pageable pageable) {

        carts.flush();

        BigDecimal subtotal = subtotal(cart);
        BigDecimal discount = BigDecimal.ZERO;

        if (cart.getAppliedPromoCode() != null) {
            try {
                discount = promos.calculate(
                                cart.getUser(),
                                cart,
                                cart.getAppliedPromoCode(),
                                BigDecimal.ZERO,
                                false)
                        .discount();

            } catch (PromoCodeInvalidException ignored) {
                cart.setAppliedPromoCode(null);
            }
        }

        Page<CartItemResponse> rows = items.findByCartId(
                        cart.getId(),
                        support.page(
                                pageable,
                                "id",
                                "quantity",
                                "priceAtAdd"))
                .map(item -> {
                    BigDecimal currentPrice = support.price(
                            item.getProduct(),
                            item.getVariant());

                    boolean priceChanged = currentPrice.compareTo(
                            item.getPriceAtAdd()) != 0;

                    return new CartItemResponse(
                            item.getId(),
                            item.getProduct().getId(),
                            item.getVariant() == null
                                    ? null
                                    : item.getVariant().getId(),
                            item.getProduct().getName(),
                            item.getQuantity(),
                            currentPrice,
                            currentPrice.multiply(
                                    BigDecimal.valueOf(item.getQuantity())),
                            item.getPriceAtAdd(),
                            priceChanged);
                });

        int totalQuantity = cart.getCartItems()
                .stream()
                .mapToInt(CartItem::getQuantity)
                .sum();

        return new CartResponse(
                cart.getId(),
                rows,
                totalQuantity,
                subtotal,
                cart.getAppliedPromoCode(),
                discount,
                subtotal.subtract(discount));
    }
    private CartItem findItem(
            Cart cart,
            Long itemId) {

        return cart.getCartItems()
                .stream()
                .filter(item -> item.getId().equals(itemId))
                .findFirst()
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Cart item not found"));
    }
}