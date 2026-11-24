package senac.com.backendTCG.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AdministradorRequest(@NotBlank @Size(max = 150) String nome) {}
