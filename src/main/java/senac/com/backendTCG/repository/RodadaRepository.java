package senac.com.backendTCG.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import senac.com.backendTCG.entity.Rodada;

import java.util.List;

@Repository
public interface RodadaRepository extends JpaRepository<Rodada, Long> {
    List<Rodada> findByTorneio_IdOrderByNumeroAsc(Long torneioId);
    boolean existsByTorneio_IdAndNumero(Long torneioId, Integer numero);
    boolean existsByTorneio_IdAndNumeroAndIdNot(Long torneioId, Integer numero, Long id);
}
