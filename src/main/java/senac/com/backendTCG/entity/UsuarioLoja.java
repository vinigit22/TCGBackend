package senac.com.backendTCG.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.LocalDateTime;

// Perfil de loja (tabela "loja"). A chave primaria e o id da propria conta.
@Entity
@Table(name = "loja")
@Getter
@Setter
public class UsuarioLoja {

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

    @Column(name = "slug", length = 160, nullable = false)
    private String slug;

    @Column(name = "descricao", columnDefinition = "TEXT")
    private String descricao;

    @Column(name = "imagem_perfil", length = 500)
    private String imagemPerfil;

    @Column(name = "imagem_banner", length = 500)
    private String imagemBanner;

    @Column(name = "telefone", length = 20)
    private String telefone;

    @ManyToOne
    @OnDelete(action = OnDeleteAction.SET_NULL) // ON DELETE SET NULL, como no script SQL
    @JoinColumn(name = "endereco_id")
    private Endereco endereco;

    @Column(name = "verificada", nullable = false)
    private Boolean verificada = false;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private LocalDateTime criadoEm;

    @Column(name = "atualizado_em", nullable = false)
    private LocalDateTime atualizadoEm;

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
