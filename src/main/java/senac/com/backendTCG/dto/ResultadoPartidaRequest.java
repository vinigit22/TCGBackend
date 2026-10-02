package senac.com.backendTCG.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import senac.com.backendTCG.entity.enums.ResultadoPartida;

// Se a partida tiver games cadastrados, o placar vem deles e os campos de games abaixo sao ignorados
public record ResultadoPartidaRequest(
        @PositiveOrZero Integer gamesA,
        @PositiveOrZero Integer gamesB,
        @PositiveOrZero Integer gamesEmpate,
        @NotNull ResultadoPartida resultado
) {}
