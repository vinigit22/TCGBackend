package senac.com.backendTCG.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "administrador")
@Getter
@Setter
public class Administrador {

    @Id
    @Column(name = "conta_id")
    private Long contaId;

    @JsonIgnore
    @MapsId
    @OneToOne(optional = false)
    @JoinColumn(name = "conta_id")
    private Conta conta;

    @Column(name = "nome", length = 150, nullable = false)
    private String nome;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private LocalDateTime criadoEm;

    @PrePersist
    void prePersist() {
        criadoEm = LocalDateTime.now();
    }
}
