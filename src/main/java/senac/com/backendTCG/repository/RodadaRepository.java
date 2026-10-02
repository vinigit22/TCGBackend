package senac.com.backendTCG.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import senac.com.backendTCG.entity.Rodada;

import java.util.List;
import java.util.Optional;

@Repository
public interface RodadaRepository extends JpaRepository<Rodada, Long> {
    List<Rodada> findByTorneio_IdOrderByNumeroAsc(Long torneioId);
    Optional<Rodada> findByTorneio_IdAndNumero(Long torneioId, Integer numero);
    boolean existsByTorneio_Id(Long torneioId);
    boolean existsByTorneio_IdAndNumero(Long torneioId, Integer numero);
    boolean existsByTorneio_IdAndNumeroAndIdNot(Long torneioId, Integer numero, Long id);
}
