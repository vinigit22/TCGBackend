package senac.com.backendTCG.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

// Classificacao final do jogador no torneio (base da pagina de trofeus)
@Entity
@Table(name = "torneio_resultado")
@Getter
@Setter
public class TorneioResultado {

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

    // 1 = ouro, 2 = prata, 3 = bronze
    @Column(name = "colocacao", nullable = false)
    private Integer colocacao;

    @Column(name = "vitorias", nullable = false)
    private Integer vitorias = 0;

    @Column(name = "derrotas", nullable = false)
    private Integer derrotas = 0;

    @Column(name = "empates", nullable = false)
    private Integer empates = 0;

    @Column(name = "premio_recebido", length = 255)
    private String premioRecebido;

    @Column(name = "registrado_em", nullable = false, updatable = false)
    private LocalDateTime registradoEm;

    @PrePersist
    void prePersist() {
        registradoEm = LocalDateTime.now();
    }
}
