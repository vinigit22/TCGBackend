package senac.com.backendTCG.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import senac.com.backendTCG.entity.Game;

import java.util.List;

@Repository
public interface GameRepository extends JpaRepository<Game, Long> {
    List<Game> findByPartida_IdOrderByNumeroAsc(Long partidaId);
    boolean existsByPartida_IdAndNumero(Long partidaId, Integer numero);
    boolean existsByPartida_IdAndNumeroAndIdNot(Long partidaId, Integer numero, Long id);
}
