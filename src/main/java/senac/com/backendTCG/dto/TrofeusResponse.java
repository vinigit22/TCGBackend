package senac.com.backendTCG.dto;

// Linha da view vw_trofeus
public record TrofeusResponse(
        Long jogadorId,
        String nickname,
        String nome,
        Long ouro,
        Long prata,
        Long bronze,
        Long torneiosDisputados
) {}
