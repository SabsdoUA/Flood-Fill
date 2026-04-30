package sk.tuke.gamestudio.authentication.api.dto;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LoginRequest {

    @NotBlank(message = "E-mail je povinný")
    @Pattern(regexp = "^[A-Za-z0-9._%+-]+@gmail\\.com$", message = "Povolené sú iba adresy Gmail")
    private String email;

    @NotBlank(message = "Heslo je povinné")
    @Size(min = 8, max = 72, message = "Heslo musí mať od 8 do 72 znakov")
    private String password;
}