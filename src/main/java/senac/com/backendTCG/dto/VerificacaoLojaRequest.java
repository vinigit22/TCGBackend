package senac.com.backendTCG.dto;

import jakarta.validation.constraints.NotNull;

public record VerificacaoLojaRequest(@NotNull Boolean verificada) {}
