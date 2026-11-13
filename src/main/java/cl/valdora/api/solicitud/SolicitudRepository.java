package cl.valdora.api.solicitud;

import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public interface SolicitudRepository extends JpaRepository<SolicitudServicio, Long>, JpaSpecificationExecutor<SolicitudServicio> {

    boolean existsByClienteId(Long clienteId);

    boolean existsByProductoId(Long productoId);

    /** Carga cliente y producto en la misma consulta (evita el problema N+1). */
    @Override
    @EntityGraph(attributePaths = {"cliente", "producto"})
    Page<SolicitudServicio> findAll(Specification<SolicitudServicio> filtro, Pageable pagina);

    @Override
    @EntityGraph(attributePaths = {"cliente", "producto"})
    Optional<SolicitudServicio> findById(Long id);

    /** Filtros combinables: estado, prioridad, cliente y rango de fechas de creación (ambos inclusive). */
    static Specification<SolicitudServicio> filtro(EstadoSolicitud estado, Prioridad prioridad, Long clienteId,
                                                   LocalDate desde, LocalDate hasta) {
        return (root, query, cb) -> {
            List<Predicate> condiciones = new ArrayList<>();
            if (estado != null) {
                condiciones.add(cb.equal(root.get("estado"), estado));
            }
            if (prioridad != null) {
                condiciones.add(cb.equal(root.get("prioridad"), prioridad));
            }
            if (clienteId != null) {
                condiciones.add(cb.equal(root.get("cliente").get("id"), clienteId));
            }
            if (desde != null) {
                condiciones.add(cb.greaterThanOrEqualTo(root.get("creadaEn"), desde.atStartOfDay()));
            }
            if (hasta != null) {
                condiciones.add(cb.lessThan(root.get("creadaEn"), hasta.plusDays(1).atStartOfDay()));
            }
            return cb.and(condiciones.toArray(Predicate[]::new));
        };
    }
}
