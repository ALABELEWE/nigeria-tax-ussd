package com.taxhelp.nigerian_tax_ussd.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.taxhelp.nigerian_tax_ussd.common.ApiResponse;
import com.taxhelp.nigerian_tax_ussd.config.AdminProperties;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class AdminKeyAuthFilter extends OncePerRequestFilter {

    private static final String HEADER = "X-Admin-Key";
    private static final String ADMIN_PATH = "/api/v1/admin";

    private final AdminProperties adminProperties;
    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain)
            throws ServletException, IOException {

        if (!request.getRequestURI().startsWith(ADMIN_PATH)) {
            chain.doFilter(request, response);
            return;
        }

        String key = request.getHeader(HEADER);

        if (key == null || key.isBlank() || !key.equals(adminProperties.getKey())) {
            log.warn("Unauthorized admin access attempt from {}", request.getRemoteAddr());
            response.setStatus(HttpStatus.UNAUTHORIZED.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write(
                objectMapper.writeValueAsString(ApiResponse.error("Invalid or missing X-Admin-Key"))
            );
            return;
        }

        chain.doFilter(request, response);
    }
}