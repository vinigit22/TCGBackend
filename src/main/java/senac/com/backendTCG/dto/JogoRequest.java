package senac.com.backendTCG.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// Slug vazio = gerado a partir do nome
public record JogoRequest(
        @NotBlank @Size(max = 100) String nome,
        @Size(max = 100) String slug,
        @Size(max = 500) String icone,
        Boolean ativo
) {}
