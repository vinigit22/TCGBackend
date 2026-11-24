package senac.com.backendTCG.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import senac.com.backendTCG.entity.UsuarioLoja;

import java.util.List;
import java.util.Optional;

@Repository
public interface UsuarioLojaRepository extends JpaRepository<UsuarioLoja, Long> {
    List<UsuarioLoja> findByConta_DeletadoEmIsNullOrderByNomeAsc();
    Optional<UsuarioLoja> findBySlugAndConta_DeletadoEmIsNull(String slug);
    boolean existsBySlug(String slug);
    boolean existsBySlugAndContaIdNot(String slug, Long contaId);
}
