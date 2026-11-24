package senac.com.backendTCG.dto;

import java.time.LocalDateTime;

// Linha da view vw_agenda_loja (categoria = EVENTO ou TORNEIO)
public record AgendaLojaResponse(
        String categoria,
        Long id,
        Long lojaId,
        String titulo,
        String imagem,
        LocalDateTime dataInicio,
        String status,
        String jogo
) {}
