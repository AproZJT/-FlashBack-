package com.flashback.backend.auth;

import com.flashback.backend.exception.BizException;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class AuthInterceptor implements HandlerInterceptor {
    private final JwtService jwtService;

    public AuthInterceptor(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String uri = request.getRequestURI();
        if (uri.startsWith("/api/auth/login")) {
            return true;
        }

        String auth = request.getHeader("Authorization");
        if (auth == null || !auth.startsWith("Bearer ")) {
            throw new BizException("未登录或 token 缺失");
        }

        String token = auth.substring("Bearer ".length());
        Claims claims;
        try {
            claims = jwtService.parse(token);
        } catch (Exception e) {
            throw new BizException("token 无效或已过期");
        }

        String userId = claims.getSubject();
        if (userId == null || userId.isBlank()) {
            throw new BizException("token 无效");
        }
        CurrentUserContext.set(userId);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        CurrentUserContext.clear();
    }
}
