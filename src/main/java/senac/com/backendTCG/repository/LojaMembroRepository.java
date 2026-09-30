package senac.com.backendTCG.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import senac.com.backendTCG.entity.LojaMembro;
import senac.com.backendTCG.entity.enums.PapelMembro;

import java.util.List;

@Repository
public interface LojaMembroRepository extends JpaRepository<LojaMembro, Long> {
    List<LojaMembro> findByLoja_ContaId(Long lojaId);
    boolean existsByLoja_ContaIdAndConta_Id(Long lojaId, Long contaId);
    boolean existsByLoja_ContaIdAndConta_IdAndAtivoTrue(Long lojaId, Long contaId);
    boolean existsByLoja_ContaIdAndConta_IdAndPapelAndAtivoTrue(Long lojaId, Long contaId, PapelMembro papel);
    boolean existsByConta_IdAndAtivoTrue(Long contaId);
}
