package senac.com.backendTCG.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import senac.com.backendTCG.entity.enums.TipoNotificacao;

public record NotificacaoRequest(
        @NotNull Long contaId,
        @NotNull TipoNotificacao tipo,
        @NotBlank @Size(max = 150) String titulo,
        @NotBlank @Size(max = 500) String mensagem,
        Long torneioId,
        Long eventoId,
        Long partidaId
) {}
