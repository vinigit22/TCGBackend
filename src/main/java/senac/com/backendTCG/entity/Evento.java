package senac.com.backendTCG.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import senac.com.backendTCG.entity.enums.StatusEvento;
import senac.com.backendTCG.entity.enums.TipoEvento;

import java.time.LocalDateTime;

// Evento da loja que nao e torneio (troca, lancamento, confraternizacao...)
@Entity
@Table(name = "evento")
@Getter
@Setter
public class Evento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "loja_id", nullable = false)
    private UsuarioLoja loja;

    @Column(name = "titulo", length = 180, nullable = false)
    private String titulo;

    @Column(name = "descricao", columnDefinition = "TEXT")
    private String descricao;

    @Column(name = "imagem", length = 500)
    private String imagem;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false)
    private TipoEvento tipo = TipoEvento.OUTRO;

    // null = sem limite de participantes
    @Column(name = "vagas_max")
    private Integer vagasMax;

    // null = usar o endereco da loja
    @ManyToOne
    @OnDelete(action = OnDeleteAction.SET_NULL) // ON DELETE SET NULL, como no script SQL
    @JoinColumn(name = "endereco_id")
    private Endereco endereco;

    @Column(name = "data_inicio", nullable = false)
    private LocalDateTime dataInicio;

    @Column(name = "data_fim")
    private LocalDateTime dataFim;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private StatusEvento status = StatusEvento.RASCUNHO;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private LocalDateTime criadoEm;

    @Column(name = "atualizado_em", nullable = false)
    private LocalDateTime atualizadoEm;

    @Column(name = "deletado_em")
    private LocalDateTime deletadoEm;

    @PrePersist
    void prePersist() {
        criadoEm = LocalDateTime.now();
        atualizadoEm = criadoEm;
    }

    @PreUpdate
    void preUpdate() {
        atualizadoEm = LocalDateTime.now();
    }
}
