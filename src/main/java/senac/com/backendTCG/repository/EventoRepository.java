package senac.com.backendTCG.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import senac.com.backendTCG.entity.Evento;
import senac.com.backendTCG.entity.enums.StatusEvento;

import java.util.List;
import java.util.Optional;

@Repository
public interface EventoRepository extends JpaRepository<Evento, Long> {

    // Filtros opcionais: parametro null = nao filtra
    @Query("""
            select e from Evento e
            where e.deletadoEm is null
              and (:lojaId is null or e.loja.contaId = :lojaId)
              and (:status is null or e.status = :status)
            order by e.dataInicio
            """)
    List<Evento> filtrar(@Param("lojaId") Long lojaId,
                         @Param("status") StatusEvento status);

    Optional<Evento> findByIdAndDeletadoEmIsNull(Long id);
}
