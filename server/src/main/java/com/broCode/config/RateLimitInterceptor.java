package com.broCode.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * HTTP-level rate limiter applied before controllers execute.
 *
 * Limits:
 *   POST /api/user/login    → 10 attempts per IP per minute
 *   POST /api/user/register → 5 attempts per IP per 10 minutes
 *   POST /api/bro/broCode   → 3 requests per authenticated userId per minute
 *
 * Buckets are lazily created and stored in ConcurrentHashMaps (one per endpoint).
 * For the 1k-user target, each map holds at most ~1k entries — well within heap budget.
 * If horizontal scaling is added later, swap the maps for a Redis-backed Bucket4j proxy store.
 */
@Slf4j
@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    private final ConcurrentHashMap<String, Bucket> loginBuckets    = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Bucket> registerBuckets = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Bucket> chatBuckets     = new ConcurrentHashMap<>();

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) throws Exception {
        return switch (request.getServletPath()) {
            case "/api/user/login"    -> check(clientIp(request), loginBuckets,    this::loginBucket,    response);
            case "/api/user/register" -> check(clientIp(request), registerBuckets, this::registerBucket, response);
            case "/api/bro/broCode"   -> checkChat(response);
            default                   -> true;
        };
    }

    // ── Endpoint-specific bucket factories ─────────────────────────────────

    private Bucket loginBucket() {
        return Bucket.builder()
                .addLimit(Bandwidth.builder()
                        .capacity(10)
                        .refillIntervally(10, Duration.ofMinutes(1))
                        .build())
                .build();
    }

    private Bucket registerBucket() {
        return Bucket.builder()
                .addLimit(Bandwidth.builder()
                        .capacity(5)
                        .refillIntervally(5, Duration.ofMinutes(10))
                        .build())
                .build();
    }

    private Bucket chatBucket() {
        return Bucket.builder()
                .addLimit(Bandwidth.builder()
                        .capacity(3)
                        .refillIntervally(3, Duration.ofMinutes(1))
                        .build())
                .build();
    }

    // ── Internal helpers ────────────────────────────────────────────────────

    private boolean checkChat(HttpServletResponse response) throws IOException {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || !(auth.getPrincipal() instanceof String userId)) {
            return true; // unauthenticated — let JwtFilter / Spring Security handle it
        }
        return check(userId, chatBuckets, this::chatBucket, response);
    }

    private boolean check(String key,
                          ConcurrentHashMap<String, Bucket> buckets,
                          Supplier<Bucket> factory,
                          HttpServletResponse response) throws IOException {
        Bucket bucket = buckets.computeIfAbsent(key, k -> factory.get());
        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);

        if (probe.isConsumed()) return true;

        long retryAfter = TimeUnit.NANOSECONDS.toSeconds(probe.getNanosToWaitForRefill()) + 1;
        log.warn("Rate limit exceeded for key '{}' — retry in {}s", key, retryAfter);
        response.setStatus(429);
        response.setHeader("Retry-After", String.valueOf(retryAfter));
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write(
                "{\"message\":\"Too many requests — slow down, bro. Retry in "
                + retryAfter + "s.\",\"success\":false}");
        return false;
    }

    private String clientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
