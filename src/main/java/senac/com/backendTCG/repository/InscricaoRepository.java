package senac.com.backendTCG.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import senac.com.backendTCG.entity.Inscricao;
import senac.com.backendTCG.entity.enums.StatusInscricao;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface InscricaoRepository extends JpaRepository<Inscricao, Long> {

    @Query("""
            select i from Inscricao i
            where (:torneioId is null or i.torneio.id = :torneioId)
              and (:jogadorId is null or i.jogador.contaId = :jogadorId)
              and (:status is null or i.status = :status)
            order by i.inscritoEm
            """)
    List<Inscricao> filtrar(@Param("torneioId") Long torneioId,
                            @Param("jogadorId") Long jogadorId,
                            @Param("status") StatusInscricao status);

    Optional<Inscricao> findByTorneio_IdAndJogador_ContaId(Long torneioId, Long jogadorId);

    List<Inscricao> findByTorneio_IdAndStatusIn(Long torneioId, Collection<StatusInscricao> status);

    long countByTorneio_IdAndStatusIn(Long torneioId, Collection<StatusInscricao> status);

    Optional<Inscricao> findFirstByTorneio_IdAndStatusOrderByInscritoEmAsc(Long torneioId, StatusInscricao status);

    boolean existsByTorneio_IdAndSeedAndIdNot(Long torneioId, Integer seed, Long id);
}
