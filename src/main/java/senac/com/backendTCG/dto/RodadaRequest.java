package senac.com.backendTCG.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import senac.com.backendTCG.entity.enums.StatusRodada;

// torneioId e usado apenas na criacao
public record RodadaRequest(
        Long torneioId,
        @NotNull @Positive Integer numero,
        @NotBlank @Size(max = 60) String nome,
        StatusRodada status
) {}
