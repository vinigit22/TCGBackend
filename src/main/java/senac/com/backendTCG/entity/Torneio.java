package senac.com.backendTCG.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Formula;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import senac.com.backendTCG.entity.enums.StatusTorneio;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "torneio")
@Getter
@Setter
public class Torneio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "loja_id", nullable = false)
    private UsuarioLoja loja;

    @ManyToOne(optional = false)
    @JoinColumn(name = "jogo_id", nullable = false)
    private Jogo jogo;

    @ManyToOne
    @JoinColumn(name = "formato_id")
    private Formato formato;

    @Column(name = "titulo", length = 180, nullable = false)
    private String titulo;

    @Column(name = "descricao", columnDefinition = "TEXT")
    private String descricao;

    @Column(name = "imagem", length = 500)
    private String imagem;

    // Precisa ser potencia de 2 (2, 4, 8 ... 256) - ck_torneio_vagas
    @Column(name = "vagas_max", nullable = false)
    private Integer vagasMax;

    @Column(name = "taxa_inscricao", precision = 10, scale = 2, nullable = false)
    private BigDecimal taxaInscricao = BigDecimal.ZERO;

    @Column(name = "premiacao", columnDefinition = "TEXT")
    private String premiacao;

    @ManyToOne
    @OnDelete(action = OnDeleteAction.SET_NULL) // ON DELETE SET NULL, como no script SQL
    @JoinColumn(name = "endereco_id")
    private Endereco endereco;

    @Column(name = "inscricoes_ate")
    private LocalDateTime inscricoesAte;

    @Column(name = "data_inicio", nullable = false)
    private LocalDateTime dataInicio;

    // Preenchido ao gerar a chave (ChaveamentoService). Quando existe, a chave foi gerada automaticamente
    // e rodadas/partidas nao aceitam mudancas estruturais manuais.
    @Column(name = "total_rodadas")
    private Integer totalRodadas;

    // Vagas ocupadas (INSCRITO ou CONFIRMADO), calculadas na mesma consulta que carrega o torneio.
    // Nulo em um torneio recem-criado, que ainda nao foi lido do banco.
    @Formula("(select count(*) from inscricao i where i.torneio_id = id and i.status in ('INSCRITO', 'CONFIRMADO'))")
    private Long vagasOcupadas;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private StatusTorneio status = StatusTorneio.RASCUNHO;

    @Column(name = "finalizado_em")
    private LocalDateTime finalizadoEm;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private LocalDateTime criadoEm;

    @Column(name = "atualizado_em", nullable = false)
    private LocalDateTime atualizadoEm;

    @Column(name = "deletado_em")
    private LocalDateTime deletadoEm;

    // Sai no JSON como "vagasDisponiveis": evita uma chamada a /torneios/{id}/vagas por torneio na listagem
    public int getVagasDisponiveis() {
        long ocupadas = vagasOcupadas == null ? 0 : vagasOcupadas;
        return (int) Math.max(vagasMax - ocupadas, 0);
    }

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
