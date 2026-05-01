package sk.tuke.gamestudio.feedback;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.OAuth2AuthenticatedPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import sk.tuke.gamestudio.feedback.dto.CreateFeedbackRequest;
import sk.tuke.gamestudio.feedback.dto.FeedbackResponse;

import java.util.List;

@RestController
@RequestMapping("/api/feedback")
@RequiredArgsConstructor
public class FeedbackController {

    private final FeedbackService feedbackService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public FeedbackResponse createFeedback(@Valid @RequestBody CreateFeedbackRequest request,
                                           Authentication authentication) {
        String userEmail = java.util.Optional.ofNullable(authentication)
                .filter(Authentication::isAuthenticated)
                .filter(auth -> !"anonymousUser".equals(auth.getName()))
                .map(this::resolveUserEmail)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Používateľ nie je prihlásený"));
        return feedbackService.createFeedback(userEmail, request);
    }

    @GetMapping
    public List<FeedbackResponse> getFeedback(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size
    ) {
        return feedbackService.getFeedback(page, size);
    }

    private String resolveUserEmail(Authentication authentication) {
        return java.util.Optional.ofNullable(authentication.getPrincipal())
                .filter(OAuth2AuthenticatedPrincipal.class::isInstance)
                .map(OAuth2AuthenticatedPrincipal.class::cast)
                .map(principal -> principal.<String>getAttribute("email"))
                .filter(email -> email != null && !email.isBlank())
                .or(() -> java.util.Optional.ofNullable(authentication.getName())
                        .filter(name -> name.contains("@") && !name.isBlank()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Používateľ nie je prihlásený"));
    }
}
