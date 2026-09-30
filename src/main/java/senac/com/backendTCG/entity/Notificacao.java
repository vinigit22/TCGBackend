package senac.com.backendTCG.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import senac.com.backendTCG.entity.enums.TipoNotificacao;

import java.time.LocalDateTime;

@Entity
@Table(name = "notificacao")
@Getter
@Setter
public class Notificacao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @JsonIgnore
    @ManyToOne(optional = false)
    @JoinColumn(name = "conta_id", nullable = false)
    private Conta conta;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false)
    private TipoNotificacao tipo;

    @Column(name = "titulo", length = 150, nullable = false)
    private String titulo;

    @Column(name = "mensagem", length = 500, nullable = false)
    private String mensagem;

    // Referencias opcionais para o front abrir a tela certa ao clicar na notificacao
    @Column(name = "torneio_id")
    private Long torneioId;

    @Column(name = "evento_id")
    private Long eventoId;

    @Column(name = "partida_id")
    private Long partidaId;

    @Column(name = "lida", nullable = false)
    private Boolean lida = false;

    @Column(name = "lida_em")
    private LocalDateTime lidaEm;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private LocalDateTime criadoEm;

    @PrePersist
    void prePersist() {
        criadoEm = LocalDateTime.now();
    }
}
