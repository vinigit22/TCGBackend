
package senac.com.backendTCG.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "usuarioloja")
@Getter
@Setter
public class UsuarioLoja {

    @Id
    @GeneratedValue
    @Column(name = "ID")
    private UUID id;

    @Column(name = "NOMELOJA", length = 15, nullable = false)
    private String nomeLoja;

    @Column(name = "EMAIL", length = 30, nullable = false)
    private String email;

    @Column(name = "SENHA_HASH", nullable = false)
    private String senhaHash;

    @Column(name = "FOTO_PERFIL", columnDefinition = "TEXT")
    private String fotoPerfil;

    @Column(name = "BIO", columnDefinition = "TEXT")
    private String bio;
    
    @Column(name = "CRIADO_EM", nullable = false)
    private LocalDateTime criadoEm;

}