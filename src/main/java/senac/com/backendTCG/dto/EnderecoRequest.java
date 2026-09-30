package senac.com.backendTCG.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record EnderecoRequest(
        @Pattern(regexp = "\\d{8}", message = "deve conter 8 digitos, sem traco") String cep,
        @NotBlank @Size(max = 200) String logradouro,
        @Size(max = 20) String numero,
        @Size(max = 100) String complemento,
        @Size(max = 100) String bairro,
        @NotBlank @Size(max = 120) String cidade,
        @NotBlank @Size(min = 2, max = 2) String estado,
        BigDecimal latitude,
        BigDecimal longitude,
        @Size(max = 255) String referencia
) {}
