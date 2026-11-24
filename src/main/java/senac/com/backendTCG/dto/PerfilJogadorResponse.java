package senac.com.backendTCG.dto;

import senac.com.backendTCG.entity.UsuarioJogador;

import java.time.LocalDate;

// Perfil completo do jogador logado (GET /jogadores/me), com os dados que nao saem no perfil publico.
// Tem os mesmos campos do PUT /jogadores/{id}: o front le aqui, altera e devolve tudo.
public record PerfilJogadorResponse(
        Long contaId,
        String email,
        String nome,
        String nickname,
        String imagemPerfil,
        String bio,
        LocalDate dataNascimento,
        String cidade,
        String estado
) {
    public static PerfilJogadorResponse de(UsuarioJogador jogador) {
        return new PerfilJogadorResponse(jogador.getContaId(), jogador.getConta().getEmail(), jogador.getNome(),
                jogador.getNickname(), jogador.getImagemPerfil(), jogador.getBio(), jogador.getDataNascimento(),
                jogador.getCidade(), jogador.getEstado());
    }
}
