package senac.com.backendTCG.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDateTime;

// lojaId e usado apenas na criacao. O status muda por PUT /torneios/{id}/status.
public record TorneioRequest(
        Long lojaId,
        @NotNull Integer jogoId,
        Integer formatoId,
        @NotBlank @Size(max = 180) String titulo,
        String descricao,
        @Size(max = 500) String imagem,
        @NotNull Integer vagasMax,
        @PositiveOrZero BigDecimal taxaInscricao,
        String premiacao,
        Long enderecoId,
        LocalDateTime inscricoesAte,
        @NotNull LocalDateTime dataInicio
) {}
