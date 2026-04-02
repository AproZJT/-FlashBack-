package com.flashback.backend.config;

import com.flashback.backend.auth.AuthInterceptor;
import com.flashback.backend.auth.CurrentUserIdArgumentResolver;
import com.flashback.backend.metrics.RequestMetricsInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

@Configuration
public class AuthWebMvcConfig implements WebMvcConfigurer {
    private final AuthInterceptor authInterceptor;
    private final RequestMetricsInterceptor requestMetricsInterceptor;
    private final CurrentUserIdArgumentResolver currentUserIdArgumentResolver;

    public AuthWebMvcConfig(AuthInterceptor authInterceptor,
                            RequestMetricsInterceptor requestMetricsInterceptor,
                            CurrentUserIdArgumentResolver currentUserIdArgumentResolver) {
        this.authInterceptor = authInterceptor;
        this.requestMetricsInterceptor = requestMetricsInterceptor;
        this.currentUserIdArgumentResolver = currentUserIdArgumentResolver;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(requestMetricsInterceptor)
                .addPathPatterns("/api/**");
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns("/api/auth/login");
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(currentUserIdArgumentResolver);
    }
}
