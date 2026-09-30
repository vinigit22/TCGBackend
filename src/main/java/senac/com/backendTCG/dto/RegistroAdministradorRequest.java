package senac.com.backendTCG.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegistroAdministradorRequest(
        @NotBlank @Email @Size(max = 255) String email,
        @NotBlank @Size(min = 6, max = 100) String senha,
        @NotBlank @Size(max = 150) String nome
) {}
