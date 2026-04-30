package sk.tuke.gamestudio.infrastructure.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RequestLoggingFilterTest {

    private final RequestLoggingFilter filter = new RequestLoggingFilter();

    @Test
    void givenRequest_whenDoFilterInternal_thenSetsHeaderInvokesChainAndClearsMdc() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        when(request.getMethod()).thenReturn("GET");
        when(request.getRequestURI()).thenReturn("/api/ping");
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
        when(response.getStatus()).thenReturn(200);

        filter.doFilterInternal(request, response, chain);

        verify(chain).doFilter(request, response);
        verify(response).setHeader(org.mockito.ArgumentMatchers.eq("X-Request-Id"), org.mockito.ArgumentMatchers.matches("[a-f0-9]{8}"));
        assertThat(MDC.get("requestId")).isNull();
    }

    @Test
    void givenChainThrows_whenDoFilterInternal_thenStillClearsMdc() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);
        FilterChain chain = mock(FilterChain.class);

        when(request.getMethod()).thenReturn("POST");
        when(request.getRequestURI()).thenReturn("/api/fail");
        when(request.getRemoteAddr()).thenReturn("10.0.0.1");
        when(response.getStatus()).thenReturn(500);
        doThrow(new ServletException("broken")).when(chain).doFilter(request, response);

        assertThatThrownBy(() -> filter.doFilterInternal(request, response, chain))
                .isInstanceOf(ServletException.class)
                .hasMessage("broken");

        assertThat(MDC.get("requestId")).isNull();
    }
}
