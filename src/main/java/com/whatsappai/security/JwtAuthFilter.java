package com.whatsappai.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
@Slf4j
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserDetailsServiceImpl userDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res,
                                    FilterChain chain) throws ServletException, IOException {
        String token = null;
        String header = req.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            token = header.substring(7);
        } else if (req.getRequestURI().contains("/sse/events")) {
            token = req.getParameter("token");
        }

        if (token == null) {
            chain.doFilter(req, res);
            return;
        }

        try {
            Claims claims = jwtService.validateAndExtract(token);
            String email = claims.getSubject();
            String businessId = claims.get("businessId", String.class);
            String role = claims.get("role", String.class);

            UserDetails userDetails = userDetailsService.loadUserByUsername(email);

            UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(
                    userDetails, null,
                    List.of(new SimpleGrantedAuthority("ROLE_" + role)));

            // businessId stored in details — never from request body
            auth.setDetails(businessId);
            SecurityContextHolder.getContext().setAuthentication(auth);
        } catch (JwtException e) {
            log.warn("Invalid JWT: {}", e.getMessage());
        }
        chain.doFilter(req, res);
    }
}
