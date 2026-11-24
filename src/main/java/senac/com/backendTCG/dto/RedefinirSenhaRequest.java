package senac.com.backendTCG.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// token = codigo recebido por email em POST /auth/esqueci-senha
public record RedefinirSenhaRequest(
        @NotBlank String token,
        @NotBlank @Size(min = 6, max = 100) String novaSenha
) {}
