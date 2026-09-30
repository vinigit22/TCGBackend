package senac.com.backendTCG.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "jogo")
@Getter
@Setter
public class Jogo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Integer id;

    @Column(name = "nome", length = 100, nullable = false)
    private String nome;

    @Column(name = "slug", length = 100, nullable = false)
    private String slug;

    @Column(name = "icone", length = 500)
    private String icone;

    @Column(name = "ativo", nullable = false)
    private Boolean ativo = true;
}
