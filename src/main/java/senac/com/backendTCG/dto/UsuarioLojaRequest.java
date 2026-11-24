package senac.com.backendTCG.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// Atualizacao do perfil da loja (email e senha sao alterados em /contas)
public record UsuarioLojaRequest(
        @NotBlank @Size(max = 150) String nome,
        @Size(max = 160) String slug,
        String descricao,
        @Size(max = 500) String imagemPerfil,
        @Size(max = 500) String imagemBanner,
        @Size(max = 20) String telefone,
        Long enderecoId
) {}
