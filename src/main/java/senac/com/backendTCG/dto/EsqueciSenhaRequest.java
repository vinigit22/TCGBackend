package senac.com.backendTCG.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record EsqueciSenhaRequest(@NotBlank @Email String email) {}
