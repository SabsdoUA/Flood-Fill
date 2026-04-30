package sk.tuke.gamestudio.authentication.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ResetPasswordRequest {
    @NotBlank(message = "Token je povinný")
    private String token;

    @NotBlank(message = "Heslo je povinné")
    @Size(min = 8, message = "Heslo musí mať aspoň 8 znakov")
    @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$", message = "Heslo musí obsahovať písmeno aj číslo")
    private String newPassword;
}
