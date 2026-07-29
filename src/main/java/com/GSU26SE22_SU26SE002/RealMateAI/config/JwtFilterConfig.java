package com.GSU26SE22_SU26SE002.RealMateAI.config;

import com.GSU26SE22_SU26SE002.RealMateAI.service_implements.JwtServiceImplement;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.SignatureException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

@Component
public class JwtFilterConfig extends OncePerRequestFilter {

    @Autowired
    @Lazy
    private JwtServiceImplement jwtService;

    @Autowired
    @Lazy
    private UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String requestURI = request.getRequestURI();
        String method = request.getMethod();

        if (requestURI.equals("/")
                || requestURI.equals("/login")
                || requestURI.equals("/index.html")
                || requestURI.endsWith(".js")
                || requestURI.endsWith(".css")
                || requestURI.endsWith(".png")
                || requestURI.endsWith(".jpg")
                || requestURI.endsWith(".webp")
                || requestURI.endsWith(".gif")
                || requestURI.endsWith(".ico")
                || requestURI.startsWith("/swagger-ui")
                || requestURI.startsWith("/v3/api-docs")
                || requestURI.startsWith("/webjars/")
                || requestURI.equals("/auth/login")
                || requestURI.equals("/auth/verify-login")
                || requestURI.equals("/auth/activate-account")
                || requestURI.equals("/auth/register")
                || requestURI.equals("/auth/verify-otp")
                || requestURI.equals("/auth/send-otp")
                || requestURI.equals("/auth/forgot-password")
                || requestURI.equals("/auth/new-password")
                || requestURI.equals("/provinces")
                || requestURI.equals("/wards")
                || requestURI.equals("/property-types")
                || requestURI.equals("/property-conditions")
                || requestURI.equals("/strategies")
                || requestURI.equals("/api/chat")
                || requestURI.equals("/error")
                || requestURI.equals("/posting-packages/active")
                || requestURI.equals("/membership-plans/active")
                || requestURI.equals("/wallets/deposit/success")
                || requestURI.equals("/wallets/deposit/cancel")
                || requestURI.equals("/wallets/deposit/webhook")
                || (requestURI.startsWith("/listings") && "GET".equalsIgnoreCase(method))
                || (requestURI.startsWith("/investor/listings") && "GET".equalsIgnoreCase(method))
                || (requestURI.startsWith("/posting-packages/") && "GET".equalsIgnoreCase(method))
                || (requestURI.startsWith("/posting-package-categories") && "GET".equalsIgnoreCase(method)) // Bỏ qua kiểm tra Token cho các request GET tới posting-package-categories
                || (requestURI.startsWith("/membership-plans/") && "GET".equalsIgnoreCase(method) && !requestURI.contains("/admin/"))
                || (requestURI.startsWith("/media/thumbnail") && "GET".equalsIgnoreCase(method))
                || requestURI.startsWith("/locations")
                || requestURI.startsWith("/news-categories")
                || requestURI.startsWith("/news")
                | (requestURI.equals("/listings/search") && "POST".equalsIgnoreCase(method))
                || requestURI.startsWith("/ward-boundary")) {
            filterChain.doFilter(request, response);
            return;
        }

        final String authorizationHeader = request.getHeader("Authorization");
        String token = null;
        String username = null;
        String role = null;

        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            handleErrorResponse(response, HttpStatus.UNAUTHORIZED, "MISSING_TOKEN",
                    "Missing token for authentication. Please log in to obtain a valid token.");
            return;
        }

        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            token = authorizationHeader.substring(7);
            try {
                username = jwtService.extractUsername(token);
                role = jwtService.extractRole(token);
            } catch (ExpiredJwtException e) {
                handleErrorResponse(response, HttpStatus.UNAUTHORIZED, "TOKEN_EXPIRED",
                        "The token has expired. Please log in again to get a new token.");
                return;
            } catch (SignatureException e) {
                handleErrorResponse(response, HttpStatus.UNAUTHORIZED, "INVALID_SIGNATURE",
                        "Invalid token signature. The token may have been tampered with.");
                return;
            } catch (MalformedJwtException | IllegalArgumentException e) {
                handleErrorResponse(response, HttpStatus.UNAUTHORIZED, "MALFORMED_TOKEN",
                        "The token format is malformed or invalid.");
                return;
            } catch (Exception e) {
                handleErrorResponse(response, HttpStatus.UNAUTHORIZED, "AUTH_ERROR",
                        "An error occurred while parsing the authentication token.");
                return;
            }
        }

        try {
            if (username != null && role != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                UserDetails userDetails = userDetailsService.loadUserByUsername(username);

                if (jwtService.validateToken(token, userDetails.getUsername(), role)) {
                    String cleanRole = role.trim();
                    if (cleanRole.startsWith("ROLE_") || cleanRole.startsWith("role_")) {
                        cleanRole = cleanRole.substring(5);
                    }

                    System.out.println("👉 QUYỀN ĐANG NẠP VÀO SPRING LÀ: ROLE_" + cleanRole);

                    List<SimpleGrantedAuthority> authorities = Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + cleanRole));
                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(userDetails, null, authorities);

                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                } else {
                    handleErrorResponse(response, HttpStatus.UNAUTHORIZED, "VALIDATION_FAILED",
                            "Token validation failed against system records.");
                    return;
                }
            }
        } catch (UsernameNotFoundException e) {
            handleErrorResponse(response, HttpStatus.UNAUTHORIZED, "USER_NOT_FOUND",
                    "The user account associated with this token does not exist or has been deleted.");
            return;
        } catch (Exception e) {
            handleErrorResponse(response, HttpStatus.UNAUTHORIZED, "SERVER_AUTH_ERROR",
                    "An internal error occurred during account verification.");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private void handleErrorResponse(HttpServletResponse response, HttpStatus status, String errorCode, String message) throws IOException {
        response.setStatus(status.value());
        response.setContentType("application/json;charset=UTF-8");

        String jsonResponse = String.format("{\"status\": %d, \"error_code\": \"%s\", \"message\": \"%s\"}",
                status.value(), errorCode, message);

        response.getWriter().write(jsonResponse);
    }
}