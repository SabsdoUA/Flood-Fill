package sk.tuke.gamestudio.infrastructure.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Slf4j
@Component
public class RequestLoggingFilter extends OncePerRequestFilter {

    private static final String REQUEST_ID = "requestId";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        var requestId = UUID.randomUUID().toString().substring(0, 8);
        var startedAt = System.currentTimeMillis();

        MDC.put(REQUEST_ID, requestId);
        response.setHeader("X-Request-Id", requestId);

        try {
            filterChain.doFilter(request, response);
        } finally {
            var durationMs = System.currentTimeMillis() - startedAt;
            log.info("HTTP {} {} from {} -> {} in {} ms",
                    request.getMethod(),
                    request.getRequestURI(),
                    request.getRemoteAddr(),
                    response.getStatus(),
                    durationMs);
            MDC.remove(REQUEST_ID);
        }
    }
}
