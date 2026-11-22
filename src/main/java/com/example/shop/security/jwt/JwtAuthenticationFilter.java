package com.example.shop.security.jwt;

import com.example.shop.security.service.CustomUserDetailsService;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider tokens;
    private final CustomUserDetailsService users;

    public JwtAuthenticationFilter(
            JwtTokenProvider tokens,
            CustomUserDetailsService users) {

        this.tokens = tokens;
        this.users = users;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain chain) throws ServletException, IOException {

        String header = request.getHeader("Authorization");

        if (header != null
                && header.startsWith("Bearer ")
                && SecurityContextHolder.getContext()
                .getAuthentication() == null) {

            String token = header.substring(7);

            try {
                if (tokens.isValidAccessToken(token)) {
                    UserDetails user = users.loadUserByUsername(
                            tokens.getEmail(token));

                    if (user.isEnabled()
                            && user.isAccountNonLocked()
                            && user.isAccountNonExpired()
                            && user.isCredentialsNonExpired()) {

                        var authentication =
                                new UsernamePasswordAuthenticationToken(
                                        user,
                                        null,
                                        user.getAuthorities());

                        authentication.setDetails(
                                new WebAuthenticationDetailsSource()
                                        .buildDetails(request));

                        var context =
                                SecurityContextHolder.createEmptyContext();

                        context.setAuthentication(authentication);
                        SecurityContextHolder.setContext(context);
                    }
                }
            } catch (JwtException
                     | IllegalArgumentException
                     | UsernameNotFoundException e) {

                SecurityContextHolder.clearContext();
            }
        }

        chain.doFilter(request, response);
    }
}
