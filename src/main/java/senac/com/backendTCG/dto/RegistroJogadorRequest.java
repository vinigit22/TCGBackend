package senac.com.backendTCG.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

// Cria a conta (tipo JOGADOR) e o perfil de jogador de uma vez
public record RegistroJogadorRequest(
        @NotBlank @Email @Size(max = 255) String email,
        @NotBlank @Size(min = 6, max = 100) String senha,
        @NotBlank @Size(max = 150) String nome,
        @NotBlank @Size(max = 50) String nickname,
        @Size(max = 500) String imagemPerfil,
        @Size(max = 500) String bio,
        LocalDate dataNascimento,
        @Size(max = 120) String cidade,
        @Size(min = 2, max = 2) String estado
) {}
