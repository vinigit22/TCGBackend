package senac.com.backendTCG.dto;

import jakarta.validation.constraints.NotNull;
import senac.com.backendTCG.entity.enums.PapelMembro;

// lojaId e contaId sao usados apenas na criacao; na edicao so papel e ativo mudam
public record LojaMembroRequest(
        Long lojaId,
        Long contaId,
        @NotNull PapelMembro papel,
        Boolean ativo
) {}
