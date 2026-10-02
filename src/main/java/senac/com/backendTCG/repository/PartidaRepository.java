package senac.com.backendTCG.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import senac.com.backendTCG.entity.Partida;

import java.util.List;

@Repository
public interface PartidaRepository extends JpaRepository<Partida, Long> {
    List<Partida> findByRodada_IdOrderByMesaAsc(Long rodadaId);
    List<Partida> findByRodada_Torneio_IdOrderByRodada_NumeroAscMesaAsc(Long torneioId);
    boolean existsByRodada_IdAndMesa(Long rodadaId, Integer mesa);
    boolean existsByRodada_IdAndMesaAndIdNot(Long rodadaId, Integer mesa, Long id);

    // Partidas da rodada anterior que alimentam esta (arvore da chave)
    List<Partida> findByProximaPartidaId(Long proximaPartidaId);
}
