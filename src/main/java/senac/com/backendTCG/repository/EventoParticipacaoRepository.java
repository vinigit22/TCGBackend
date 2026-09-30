package senac.com.backendTCG.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import senac.com.backendTCG.entity.EventoParticipacao;
import senac.com.backendTCG.entity.enums.StatusParticipacao;

import java.util.List;
import java.util.Optional;

@Repository
public interface EventoParticipacaoRepository extends JpaRepository<EventoParticipacao, Long> {

    @Query("""
            select p from EventoParticipacao p
            where (:eventoId is null or p.evento.id = :eventoId)
              and (:jogadorId is null or p.jogador.contaId = :jogadorId)
            order by p.inscritoEm
            """)
    List<EventoParticipacao> filtrar(@Param("eventoId") Long eventoId,
                                     @Param("jogadorId") Long jogadorId);

    Optional<EventoParticipacao> findByEvento_IdAndJogador_ContaId(Long eventoId, Long jogadorId);

    List<EventoParticipacao> findByEvento_IdAndStatus(Long eventoId, StatusParticipacao status);

    long countByEvento_IdAndStatus(Long eventoId, StatusParticipacao status);
}
