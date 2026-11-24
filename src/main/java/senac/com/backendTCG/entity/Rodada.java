package senac.com.backendTCG.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import senac.com.backendTCG.entity.enums.StatusRodada;

import java.time.LocalDateTime;

@Entity
@Table(name = "rodada")
@Getter
@Setter
public class Rodada {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "torneio_id", nullable = false)
    private Torneio torneio;

    @Column(name = "numero", nullable = false)
    private Integer numero;

    @Column(name = "nome", length = 60, nullable = false)
    private String nome;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private StatusRodada status = StatusRodada.AGUARDANDO;

    @Column(name = "iniciada_em")
    private LocalDateTime iniciadaEm;

    @Column(name = "encerrada_em")
    private LocalDateTime encerradaEm;
}
