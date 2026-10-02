package senac.com.backendTCG.dto;

import senac.com.backendTCG.entity.Conta;
import senac.com.backendTCG.entity.enums.TipoConta;

public record LoginResponse(
        String token,
        Long contaId,
        String email,
        TipoConta tipo
) {
    public static LoginResponse de(Conta conta, String token) {
        return new LoginResponse(token, conta.getId(), conta.getEmail(), conta.getTipo());
    }
}
