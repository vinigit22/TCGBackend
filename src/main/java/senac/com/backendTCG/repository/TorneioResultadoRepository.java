package senac.com.backendTCG.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import senac.com.backendTCG.entity.TorneioResultado;

import java.util.List;

@Repository
public interface TorneioResultadoRepository extends JpaRepository<TorneioResultado, Long> {

    // Resultados de torneio excluido (soft delete) ficam de fora
    @Query("""
            select r from TorneioResultado r
            where r.torneio.deletadoEm is null
              and (:torneioId is null or r.torneio.id = :torneioId)
              and (:jogadorId is null or r.jogador.contaId = :jogadorId)
            order by r.torneio.id, r.colocacao
            """)
    List<TorneioResultado> filtrar(@Param("torneioId") Long torneioId,
                                   @Param("jogadorId") Long jogadorId);

    boolean existsByTorneio_IdAndJogador_ContaId(Long torneioId, Long jogadorId);
}
