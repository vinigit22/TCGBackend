package senac.com.backendTCG.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import senac.com.backendTCG.entity.UsuarioJogador;

import java.util.List;
import java.util.Optional;

@Repository
public interface UsuarioJogadorRepository extends JpaRepository<UsuarioJogador, Long> {
    List<UsuarioJogador> findByConta_DeletadoEmIsNullOrderByNicknameAsc();
    Optional<UsuarioJogador> findByNicknameAndConta_DeletadoEmIsNull(String nickname);
    boolean existsByNickname(String nickname);
    boolean existsByNicknameAndContaIdNot(String nickname, Long contaId);
}
