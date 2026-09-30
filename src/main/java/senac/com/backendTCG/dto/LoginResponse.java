package senac.com.backendTCG.dto;

import senac.com.backendTCG.entity.enums.TipoConta;

public record LoginResponse(
        String token,
        Long contaId,
        String email,
        TipoConta tipo
) {}
