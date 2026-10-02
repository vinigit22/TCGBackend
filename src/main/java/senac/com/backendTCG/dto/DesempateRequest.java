package senac.com.backendTCG.dto;

import jakarta.validation.constraints.NotNull;
import senac.com.backendTCG.entity.enums.SlotPartida;

// Quem venceu o game de desempate: A ou B
public record DesempateRequest(@NotNull SlotPartida vencedor) {}
