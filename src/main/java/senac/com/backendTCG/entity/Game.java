package senac.com.backendTCG.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import senac.com.backendTCG.entity.enums.ResultadoGame;

// Cada game (1 a 5) dentro de uma partida melhor-de-X
@Entity
@Table(name = "game")
@Getter
@Setter
public class Game {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @JsonIgnoreProperties({"rodada", "inscricaoA", "inscricaoB", "vencedor"})
    @ManyToOne(optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE) // ON DELETE CASCADE, como no script SQL
    @JoinColumn(name = "partida_id", nullable = false)
    private Partida partida;

    @Column(name = "numero", nullable = false)
    private Integer numero;

    @Enumerated(EnumType.STRING)
    @Column(name = "resultado", nullable = false)
    private ResultadoGame resultado;

    @Column(name = "duracao_min")
    private Integer duracaoMin;
}
