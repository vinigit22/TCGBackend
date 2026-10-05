package senac.com.backendTCG.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

// Perfil de jogador (tabela "jogador"). A chave primaria e o id da propria conta.
@Entity
@Table(name = "jogador")
@Getter
@Setter
public class UsuarioJogador {

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

    @Column(name = "nickname", length = 50, nullable = false)
    private String nickname;

    @Column(name = "imagem_perfil", length = 500)
    private String imagemPerfil;

    @Column(name = "bio", length = 500)
    private String bio;

    // Dado pessoal: fica fora do JSON publico. O proprio jogador le em GET /jogadores/me
    @JsonIgnore
    @Column(name = "data_nascimento")
    private LocalDate dataNascimento;

    @Column(name = "cidade", length = 120)
    private String cidade;

    @Column(name = "estado", length = 2)
    private String estado;

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
