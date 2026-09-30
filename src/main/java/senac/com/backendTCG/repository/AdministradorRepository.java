package senac.com.backendTCG.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import senac.com.backendTCG.entity.Administrador;

import java.util.List;

@Repository
public interface AdministradorRepository extends JpaRepository<Administrador, Long> {
    List<Administrador> findByConta_DeletadoEmIsNullOrderByNomeAsc();
}
