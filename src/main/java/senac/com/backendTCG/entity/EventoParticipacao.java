package senac.com.backendTCG.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import senac.com.backendTCG.entity.enums.StatusParticipacao;

import java.time.LocalDateTime;

@Entity
@Table(name = "evento_participacao")
@Getter
@Setter
public class EventoParticipacao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "evento_id", nullable = false)
    private Evento evento;

    @ManyToOne(optional = false)
    @JoinColumn(name = "jogador_id", nullable = false)
    private UsuarioJogador jogador;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private StatusParticipacao status = StatusParticipacao.CONFIRMADO;

    @Column(name = "inscrito_em", nullable = false)
    private LocalDateTime inscritoEm;

    @Column(name = "cancelado_em")
    private LocalDateTime canceladoEm;

    @PrePersist
    void prePersist() {
        inscritoEm = LocalDateTime.now();
    }
}
