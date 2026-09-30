package senac.com.backendTCG.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import senac.com.backendTCG.entity.Jogo;

import java.util.List;

@Repository
public interface JogoRepository extends JpaRepository<Jogo, Integer> {
    List<Jogo> findByAtivo(Boolean ativo);
    boolean existsByNome(String nome);
    boolean existsBySlug(String slug);
    boolean existsByNomeAndIdNot(String nome, Integer id);
    boolean existsBySlugAndIdNot(String slug, Integer id);
}
