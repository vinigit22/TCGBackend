package senac.com.backendTCG.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import senac.com.backendTCG.entity.Notificacao;

import java.util.List;

@Repository
public interface NotificacaoRepository extends JpaRepository<Notificacao, Long> {
    List<Notificacao> findByConta_IdOrderByCriadoEmDesc(Long contaId);
    List<Notificacao> findByConta_IdAndLidaFalseOrderByCriadoEmDesc(Long contaId);
    long countByConta_IdAndLidaFalse(Long contaId);
}
