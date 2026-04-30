package sk.tuke.gamestudio.authentication.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ResendVerificationRequest {
    @NotBlank(message = "Email je povinný")
    @Email(message = "Neplatný email")
    private String email;
}
