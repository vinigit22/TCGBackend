package senac.com.backendTCG.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import senac.com.backendTCG.entity.Formato;

import java.util.List;

@Repository
public interface FormatoRepository extends JpaRepository<Formato, Integer> {
    List<Formato> findByJogo_Id(Integer jogoId);
    boolean existsByJogo_IdAndNome(Integer jogoId, String nome);
    boolean existsByJogo_IdAndNomeAndIdNot(Integer jogoId, String nome, Integer id);
}
