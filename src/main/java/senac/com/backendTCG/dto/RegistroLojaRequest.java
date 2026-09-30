package senac.com.backendTCG.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// Cria a conta (tipo LOJA) e o perfil da loja de uma vez. Slug vazio = gerado a partir do nome.
public record RegistroLojaRequest(
        @NotBlank @Email @Size(max = 255) String email,
        @NotBlank @Size(min = 6, max = 100) String senha,
        @NotBlank @Size(max = 150) String nome,
        @Size(max = 160) String slug,
        String descricao,
        @Size(max = 500) String imagemPerfil,
        @Size(max = 500) String imagemBanner,
        @Size(max = 20) String telefone
) {}
