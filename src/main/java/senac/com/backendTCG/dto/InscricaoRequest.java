package senac.com.backendTCG.dto;

import jakarta.validation.constraints.Positive;
import senac.com.backendTCG.entity.enums.StatusInscricao;
import senac.com.backendTCG.entity.enums.StatusPagamento;

// Criacao: torneioId (+ jogadorId quando a loja inscreve alguem).
// Edicao (equipe da loja): status, pagamentoStatus e seed.
public record InscricaoRequest(
        Long torneioId,
        Long jogadorId,
        StatusInscricao status,
        StatusPagamento pagamentoStatus,
        @Positive Integer seed
) {}
