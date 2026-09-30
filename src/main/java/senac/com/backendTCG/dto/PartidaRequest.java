package senac.com.backendTCG.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import senac.com.backendTCG.entity.enums.ResultadoPartida;
import senac.com.backendTCG.entity.enums.SlotPartida;
import senac.com.backendTCG.entity.enums.StatusPartida;

// rodadaId e usado apenas na criacao. O vencedor e calculado a partir do resultado.
public record PartidaRequest(
        Long rodadaId,
        @NotNull @Positive Integer mesa,
        Long inscricaoAId,
        Long inscricaoBId,
        Long proximaPartidaId,
        SlotPartida proximoSlot,
        StatusPartida status,
        ResultadoPartida resultado,
        @PositiveOrZero Integer gamesA,
        @PositiveOrZero Integer gamesB,
        @PositiveOrZero Integer gamesEmpate,
        @Size(max = 500) String observacao
) {}
