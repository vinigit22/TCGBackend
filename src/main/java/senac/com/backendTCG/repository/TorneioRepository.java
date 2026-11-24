package senac.com.backendTCG.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import senac.com.backendTCG.entity.Torneio;
import senac.com.backendTCG.entity.enums.StatusTorneio;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface TorneioRepository extends JpaRepository<Torneio, Long> {

    // lojaId e jogoId opcionais (null = nao filtra). status nunca vem vazio: sem filtro, o service manda todos.
    @Query("""
            select t from Torneio t
            where t.deletadoEm is null
              and (:lojaId is null or t.loja.contaId = :lojaId)
              and (:jogoId is null or t.jogo.id = :jogoId)
              and t.status in :status
            order by t.dataInicio
            """)
    List<Torneio> filtrar(@Param("lojaId") Long lojaId,
                          @Param("jogoId") Integer jogoId,
                          @Param("status") Collection<StatusTorneio> status);

    Optional<Torneio> findByIdAndDeletadoEmIsNull(Long id);

    boolean existsByJogo_Id(Integer jogoId);

    boolean existsByFormato_Id(Integer formatoId);

    // Usadas pelo agendador (TorneioScheduler)
    List<Torneio> findByStatusAndInscricoesAteBeforeAndDeletadoEmIsNull(StatusTorneio status, LocalDateTime limite);

    List<Torneio> findByStatusInAndDataInicioBetweenAndDeletadoEmIsNull(Collection<StatusTorneio> status,
                                                                         LocalDateTime inicio,
                                                                         LocalDateTime fim);
}
