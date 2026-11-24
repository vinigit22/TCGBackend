package senac.com.backendTCG.dto;

import java.time.LocalDateTime;

// Linha da view vw_chaveamento + check-in da partida (checkInA / checkInB: o jogador A / B ja confirmou presenca)
public record ChaveamentoResponse(
        Long torneioId,
        Integer rodada,
        String nomeRodada,
        Long partidaId,
        Integer mesa,
        String jogadorA,
        String jogadorB,
        Integer gamesA,
        Integer gamesB,
        String resultado,
        String vencedor,
        String status,
        LocalDateTime checkInExpiraEm,
        Boolean checkInA,
        Boolean checkInB
) {}
