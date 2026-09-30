package senac.com.backendTCG.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

// torneioId e jogadorId sao usados apenas na criacao
public record TorneioResultadoRequest(
        Long torneioId,
        Long jogadorId,
        @NotNull @Min(1) Integer colocacao,
        @PositiveOrZero Integer vitorias,
        @PositiveOrZero Integer derrotas,
        @PositiveOrZero Integer empates,
        @Size(max = 255) String premioRecebido
) {}
