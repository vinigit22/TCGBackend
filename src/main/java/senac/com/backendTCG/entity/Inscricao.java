package senac.com.backendTCG.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import senac.com.backendTCG.entity.enums.StatusInscricao;
import senac.com.backendTCG.entity.enums.StatusPagamento;

import java.time.LocalDateTime;

@Entity
@Table(name = "inscricao")
@Getter
@Setter
public class Inscricao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "torneio_id", nullable = false)
    private Torneio torneio;

    @ManyToOne(optional = false)
    @JoinColumn(name = "jogador_id", nullable = false)
    private UsuarioJogador jogador;

    // A trigger trg_inscricao_controla_vagas pode trocar para LISTA_ESPERA no INSERT
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private StatusInscricao status = StatusInscricao.INSCRITO;

    @Enumerated(EnumType.STRING)
    @Column(name = "pagamento_status", nullable = false)
    private StatusPagamento pagamentoStatus = StatusPagamento.PENDENTE;

    // Posicao sorteada na chave (ChaveamentoService)
    @Column(name = "seed")
    private Integer seed;

    @Column(name = "inscrito_em", nullable = false)
    private LocalDateTime inscritoEm;

    @Column(name = "check_in_em")
    private LocalDateTime checkInEm;

    @Column(name = "cancelado_em")
    private LocalDateTime canceladoEm;

    @PrePersist
    void prePersist() {
        inscritoEm = LocalDateTime.now();
    }
}
