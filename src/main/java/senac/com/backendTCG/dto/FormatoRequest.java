package senac.com.backendTCG.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record FormatoRequest(
        @NotNull Integer jogoId,
        @NotBlank @Size(max = 100) String nome,
        Boolean ativo
) {}
