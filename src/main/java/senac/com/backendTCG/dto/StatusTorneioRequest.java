package senac.com.backendTCG.dto;

import jakarta.validation.constraints.NotNull;
import senac.com.backendTCG.entity.enums.StatusTorneio;

public record StatusTorneioRequest(@NotNull StatusTorneio status) {}
