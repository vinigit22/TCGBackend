package senac.com.backendTCG.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import senac.com.backendTCG.entity.enums.ResultadoGame;

// partidaId e usado apenas na criacao
public record GameRequest(
        Long partidaId,
        @NotNull @Min(1) @Max(5) Integer numero,
        @NotNull ResultadoGame resultado,
        @PositiveOrZero Integer duracaoMin
) {}
