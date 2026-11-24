package senac.com.backendTCG.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

// Atualizacao do perfil do jogador (email e senha sao alterados em /contas)
public record UsuarioJogadorRequest(
        @NotBlank @Size(max = 150) String nome,
        @NotBlank @Size(max = 50) String nickname,
        @Size(max = 500) String imagemPerfil,
        @Size(max = 500) String bio,
        LocalDate dataNascimento,
        @Size(max = 120) String cidade,
        @Size(min = 2, max = 2) String estado
) {}
