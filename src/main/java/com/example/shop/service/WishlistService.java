package com.example.shop.service;

import com.example.shop.dto.request.CartItemRequest;
import com.example.shop.dto.response.CartResponse;
import com.example.shop.dto.response.WishlistResponse;
import com.example.shop.exception.ResourceNotFoundException;
import com.example.shop.model.entity.Product;
import com.example.shop.model.entity.User;
import com.example.shop.model.entity.Wishlist;
import com.example.shop.repository.WishlistRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class WishlistService {

    private final WishlistRepository wishes;
    private final CartService carts;
    private final CommerceSupport support;

    public WishlistService(
            WishlistRepository wishes,
            CartService carts,
            CommerceSupport support) {

        this.wishes = wishes;
        this.carts = carts;
        this.support = support;
    }

    @Transactional(readOnly = true)
    public Page<WishlistResponse> list(
            String email,
            Pageable pageable) {

        User user = support.user(email, false);

        return wishes.findByUserId(
                        user.getId(),
                        support.page(
                                pageable,
                                "id",
                                "addedAt",
                                "priceAtAdd"))
                .map(this::response);
    }

    public WishlistResponse add(
            String email,
            Long productId) {

        User user = support.user(email, true);
        Product product = support.activeProduct(productId);

        Wishlist wish = wishes.findByUserIdAndProductId(
                        user.getId(),
                        productId)
                .orElseGet(() -> {
                    Wishlist item = new Wishlist();
                    item.setUser(user);
                    item.setProduct(product);
                    item.setPriceAtAdd(
                            support.price(product, null));

                    return wishes.saveAndFlush(item);
                });

        return response(wish);
    }

    public void delete(
            String email,
            Long productId) {

        User user = support.user(email, true);

        wishes.delete(find(user, productId));
    }

    public CartResponse move(
            String email,
            Long productId,
            Pageable pageable) {

        User user = support.user(email, true);
        Wishlist wish = find(user, productId);

        CartResponse result = carts.add(
                email,
                new CartItemRequest(productId, null, 1),
                pageable);

        wishes.delete(wish);

        return result;
    }

    private Wishlist find(
            User user,
            Long productId) {

        return wishes.findByUserIdAndProductId(
                        user.getId(),
                        productId)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Wishlist item not found"));
    }

    private WishlistResponse response(Wishlist wish) {
        return new WishlistResponse(
                wish.getId(),
                wish.getProduct().getId(),
                wish.getProduct().getName(),
                wish.getPriceAtAdd(),
                support.price(wish.getProduct(), null),
                wish.getAddedAt());
    }
}
