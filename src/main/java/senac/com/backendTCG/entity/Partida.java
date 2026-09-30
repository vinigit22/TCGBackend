package senac.com.backendTCG.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import senac.com.backendTCG.entity.enums.ResultadoPartida;
import senac.com.backendTCG.entity.enums.SlotPartida;
import senac.com.backendTCG.entity.enums.StatusPartida;

import java.time.LocalDateTime;

@Entity
@Table(name = "partida")
@Getter
@Setter
public class Partida {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE) // ON DELETE CASCADE, como no script SQL
    @JoinColumn(name = "rodada_id", nullable = false)
    private Rodada rodada;

    @Column(name = "mesa", nullable = false)
    private Integer mesa;

    // O torneio ja aparece dentro de "rodada", entao nao repetimos nas inscricoes
    @JsonIgnoreProperties("torneio")
    @ManyToOne
    @JoinColumn(name = "inscricao_a_id")
    private Inscricao inscricaoA;

    @JsonIgnoreProperties("torneio")
    @ManyToOne
    @JoinColumn(name = "inscricao_b_id")
    private Inscricao inscricaoB;

    // Partida da rodada seguinte que recebe o vencedor (arvore da chave)
    @Column(name = "proxima_partida_id")
    private Long proximaPartidaId;

    @Enumerated(EnumType.STRING)
    @Column(name = "proximo_slot")
    private SlotPartida proximoSlot;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private StatusPartida status = StatusPartida.AGUARDANDO;

    @Enumerated(EnumType.STRING)
    @Column(name = "resultado")
    private ResultadoPartida resultado;

    // null em empate ou duplo no-show (ck_partida_resultado)
    @JsonIgnoreProperties("torneio")
    @ManyToOne
    @JoinColumn(name = "vencedor_id")
    private Inscricao vencedor;

    @Column(name = "games_a", nullable = false)
    private Integer gamesA = 0;

    @Column(name = "games_b", nullable = false)
    private Integer gamesB = 0;

    @Column(name = "games_empate", nullable = false)
    private Integer gamesEmpate = 0;

    @Column(name = "observacao", length = 500)
    private String observacao;

    @Column(name = "iniciada_em")
    private LocalDateTime iniciadaEm;

    @Column(name = "finalizada_em")
    private LocalDateTime finalizadaEm;
}
