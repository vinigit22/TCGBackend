package senac.com.backendTCG.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import senac.com.backendTCG.entity.enums.ResultadoPartida;

// Parametros da procedure sp_registrar_resultado
public record ResultadoPartidaRequest(
        @NotNull @PositiveOrZero Integer gamesA,
        @NotNull @PositiveOrZero Integer gamesB,
        @PositiveOrZero Integer gamesEmpate,
        @NotNull ResultadoPartida resultado
) {}
