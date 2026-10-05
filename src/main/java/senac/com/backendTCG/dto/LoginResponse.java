package senac.com.backendTCG.dto;

import senac.com.backendTCG.entity.Conta;
import senac.com.backendTCG.entity.enums.TipoConta;

// Resposta de login, cadastro e troca de senha. Leva o resumo do perfil para o front montar a sessao
// sem outra chamada: nome (jogador, loja ou admin), nickname (so jogador) e imagemPerfil (jogador ou loja).
public record LoginResponse(
        String token,
        Long contaId,
        String email,
        TipoConta tipo,
        String nome,
        String nickname,
        String imagemPerfil
) {
    public static LoginResponse de(Conta conta, String token, String nome, String nickname, String imagemPerfil) {
        return new LoginResponse(token, conta.getId(), conta.getEmail(), conta.getTipo(), nome, nickname, imagemPerfil);
    }
}
