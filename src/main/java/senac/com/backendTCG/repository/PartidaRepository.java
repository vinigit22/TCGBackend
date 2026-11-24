package senac.com.backendTCG.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import senac.com.backendTCG.entity.Partida;
import senac.com.backendTCG.entity.enums.StatusPartida;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface PartidaRepository extends JpaRepository<Partida, Long> {
    List<Partida> findByRodada_IdOrderByMesaAsc(Long rodadaId);
    List<Partida> findByRodada_Torneio_IdOrderByRodada_NumeroAscMesaAsc(Long torneioId);
    boolean existsByRodada_IdAndMesa(Long rodadaId, Integer mesa);
    boolean existsByRodada_IdAndMesaAndIdNot(Long rodadaId, Integer mesa, Long id);

    // Partidas da rodada anterior que alimentam esta (arvore da chave)
    List<Partida> findByProximaPartidaId(Long proximaPartidaId);

    // Partidas convocadas cujo prazo de check-in acabou sem os dois confirmarem
    List<Partida> findByStatusAndCheckInExpiraEmBefore(StatusPartida status, LocalDateTime limite);

    // Partidas convocadas (janela de check-in aberta) em que o jogador ainda nao confirmou presenca.
    // So partidas PRONTA: depois que os dois confirmam a partida vira EM_ANDAMENTO.
    @Query("""
            SELECT p FROM Partida p
              LEFT JOIN p.inscricaoA ia LEFT JOIN ia.jogador ja
              LEFT JOIN p.inscricaoB ib LEFT JOIN ib.jogador jb
             WHERE p.status = senac.com.backendTCG.entity.enums.StatusPartida.PRONTA
               AND p.checkInExpiraEm > :agora
               AND ((ja.contaId = :contaId AND p.checkInAEm IS NULL)
                 OR (jb.contaId = :contaId AND p.checkInBEm IS NULL))
             ORDER BY p.checkInExpiraEm DESC
            """)
    List<Partida> findConvocadasSemCheckIn(@Param("contaId") Long contaId, @Param("agora") LocalDateTime agora);
}
