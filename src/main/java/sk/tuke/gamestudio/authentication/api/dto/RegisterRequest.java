package sk.tuke.gamestudio.authentication.api.dto;

import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RegisterRequest {

    @NotBlank(message = "E-mail je povinný")
    @Pattern(regexp = "^[A-Za-z0-9._%+-]+@gmail\\.com$", message = "Povolené sú iba adresy Gmail")
    private String email;

    @NotBlank(message = "Prezývka je povinná")
    @Size(min = 3, max = 30, message = "Prezývka musí mať od 3 do 30 znakov")
    @Pattern(regexp = "^[A-Za-z0-9_]+$", message = "Prezývka môže obsahovať iba písmená, číslice a podčiarkovník")
    private String nickname;

    @NotBlank(message = "Heslo je povinné")
    @Size(min = 8, max = 72, message = "Heslo musí mať od 8 do 72 znakov")
    @Pattern(regexp = "^(?=.*[A-Z])(?=.*[a-z])(?=.*\\d).+$",
            message = "Heslo musí obsahovať veľké písmeno, malé písmeno a číslicu")
    private String password;
}
