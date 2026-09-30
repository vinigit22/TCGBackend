package senac.com.backendTCG.dto;

// Linha da view vw_chaveamento
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
        String status
) {}
