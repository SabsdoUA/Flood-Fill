package sk.tuke.gamestudio.feedback;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.OAuth2AuthenticatedPrincipal;
import org.springframework.web.server.ResponseStatusException;
import sk.tuke.gamestudio.feedback.dto.CreateFeedbackRequest;
import sk.tuke.gamestudio.feedback.dto.FeedbackResponse;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FeedbackControllerTest {

    @Mock
    private FeedbackService feedbackService;
    @Mock
    private Authentication authentication;

    @InjectMocks
    private FeedbackController feedbackController;

    @Test
    void givenNullAuthentication_whenCreateFeedback_thenThrowUnauthorized() {
        // Given
        CreateFeedbackRequest request = new CreateFeedbackRequest(5, "Great");

        // When / Then
        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> feedbackController.createFeedback(request, null)
        );
        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);

        verifyNoInteractions(feedbackService);
    }

    @ParameterizedTest
    @CsvSource({
            "false, player@game.com",
            "true, anonymousUser"
    })
    void givenInvalidAuthenticationState_whenCreateFeedback_thenThrowUnauthorized(boolean isAuthenticated, String name) {
        // Given
        CreateFeedbackRequest request = new CreateFeedbackRequest(5, "Great");
        when(authentication.isAuthenticated()).thenReturn(isAuthenticated);
        if (isAuthenticated) {
            when(authentication.getName()).thenReturn(name);
        }

        // When / Then
        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> feedbackController.createFeedback(request, authentication)
        );
        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);

        verifyNoInteractions(feedbackService);
    }

    @Test
    void givenOAuthPrincipalWithEmail_whenCreateFeedback_thenDelegateToService() {
        // Given
        CreateFeedbackRequest request = new CreateFeedbackRequest(4, "Nice");
        OAuth2AuthenticatedPrincipal principal = mock(OAuth2AuthenticatedPrincipal.class);
        FeedbackResponse expected = new FeedbackResponse(1L, "User", 4, "Nice", Instant.now(), LocalDate.now());

        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn("oauth-user");
        when(authentication.getPrincipal()).thenReturn(principal);
        when(principal.getAttribute("email")).thenReturn("oauth@game.com");
        when(feedbackService.createFeedback("oauth@game.com", request)).thenReturn(expected);

        // When
        FeedbackResponse actual = feedbackController.createFeedback(request, authentication);

        // Then
        assertThat(actual).isEqualTo(expected);
        verify(feedbackService).createFeedback("oauth@game.com", request);
    }

    @Test
    void givenBlankOAuthEmailAndEmailLikeAuthenticationName_whenCreateFeedback_thenUseAuthenticationName() {
        // Given
        CreateFeedbackRequest request = new CreateFeedbackRequest(3, "ok");
        OAuth2AuthenticatedPrincipal principal = mock(OAuth2AuthenticatedPrincipal.class);
        FeedbackResponse expected = new FeedbackResponse(2L, "User", 3, "ok", Instant.now(), LocalDate.now());

        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn("fallback@game.com");
        when(authentication.getPrincipal()).thenReturn(principal);
        when(principal.getAttribute("email")).thenReturn(" ");
        when(feedbackService.createFeedback("fallback@game.com", request)).thenReturn(expected);

        // When
        FeedbackResponse actual = feedbackController.createFeedback(request, authentication);

        // Then
        assertThat(actual).isEqualTo(expected);
        verify(feedbackService).createFeedback("fallback@game.com", request);
    }

    @Test
    void givenNoOAuthPrincipalAndInvalidName_whenCreateFeedback_thenThrowUnauthorized() {
        // Given
        CreateFeedbackRequest request = new CreateFeedbackRequest(3, "ok");

        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getName()).thenReturn("plainUser");
        when(authentication.getPrincipal()).thenReturn(new Object());

        // When / Then
        ResponseStatusException ex = assertThrows(
                ResponseStatusException.class,
                () -> feedbackController.createFeedback(request, authentication)
        );
        assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);

        verifyNoInteractions(feedbackService);
    }

    @Test
    void givenFeedbackRequested_whenGetFeedback_thenReturnServiceData() {
        // Given
        List<FeedbackResponse> expected = List.of(
                new FeedbackResponse(1L, "A", 5, "Top", Instant.now(), LocalDate.now())
        );
        when(feedbackService.getFeedback(0, 50)).thenReturn(expected);

        // When
        List<FeedbackResponse> actual = feedbackController.getFeedback(0, 50);

        // Then
        assertThat(actual).isEqualTo(expected);
    }
}
