package senac.com.backendTCG.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import senac.com.backendTCG.entity.Conta;
import senac.com.backendTCG.entity.enums.TipoConta;

import java.util.List;
import java.util.Optional;

@Repository
public interface ContaRepository extends JpaRepository<Conta, Long> {
    boolean existsByEmail(String email);
    Optional<Conta> findByEmail(String email);
    List<Conta> findByTipo(TipoConta tipo);
}
