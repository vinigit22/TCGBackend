package senac.com.backendTCG.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import senac.com.backendTCG.entity.enums.StatusEvento;
import senac.com.backendTCG.entity.enums.TipoEvento;

import java.time.LocalDateTime;

// lojaId e usado apenas na criacao
public record EventoRequest(
        Long lojaId,
        @NotBlank @Size(max = 180) String titulo,
        String descricao,
        @Size(max = 500) String imagem,
        TipoEvento tipo,
        @Positive Integer vagasMax,
        Long enderecoId,
        @NotNull LocalDateTime dataInicio,
        LocalDateTime dataFim,
        StatusEvento status
) {}
