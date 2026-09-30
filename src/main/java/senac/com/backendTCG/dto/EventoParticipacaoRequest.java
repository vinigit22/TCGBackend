package senac.com.backendTCG.dto;

import senac.com.backendTCG.entity.enums.StatusParticipacao;

// Criacao: eventoId (+ jogadorId quando a loja inscreve alguem). Edicao: status.
public record EventoParticipacaoRequest(
        Long eventoId,
        Long jogadorId,
        StatusParticipacao status
) {}
