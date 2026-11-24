package senac.com.backendTCG.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import senac.com.backendTCG.entity.enums.PapelMembro;

public record FuncionarioRequest(
        @NotBlank @Email String email,
        @NotBlank @Size(min = 6, max = 100) String senha,
        @NotNull PapelMembro papel
) {}
