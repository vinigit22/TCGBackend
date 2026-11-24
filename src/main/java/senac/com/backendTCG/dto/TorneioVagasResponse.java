package senac.com.backendTCG.dto;

// Linha da view vw_torneio_vagas
public record TorneioVagasResponse(
        Long torneioId,
        String titulo,
        Integer vagasMax,
        Long inscritos,
        Long vagasRestantes,
        Long listaEspera,
        Long pagamentosPendentes
) {}
