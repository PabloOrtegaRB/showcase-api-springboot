package cl.valdora.api.cliente;

import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public interface ClienteRepository extends JpaRepository<Cliente, Long>, JpaSpecificationExecutor<Cliente> {

    boolean existsByRut(String rut);

    boolean existsByRutAndIdNot(String rut, Long id);

    /** Filtros combinables: texto (razón social, correo o RUT), comuna y estado. */
    static Specification<Cliente> filtro(String texto, String comuna, Boolean activo) {
        return (root, query, cb) -> {
            List<Predicate> condiciones = new ArrayList<>();
            if (texto != null && !texto.isBlank()) {
                String patron = "%" + texto.trim().toLowerCase(Locale.ROOT) + "%";
                String patronRut = "%" + texto.replace(".", "").trim().toUpperCase(Locale.ROOT) + "%";
                condiciones.add(cb.or(
                        cb.like(cb.lower(root.get("razonSocial")), patron),
                        cb.like(cb.lower(root.get("email")), patron),
                        cb.like(root.get("rut"), patronRut)));
            }
            if (comuna != null && !comuna.isBlank()) {
                condiciones.add(cb.equal(cb.lower(root.get("comuna")), comuna.trim().toLowerCase(Locale.ROOT)));
            }
            if (activo != null) {
                condiciones.add(cb.equal(root.get("activo"), activo));
            }
            return cb.and(condiciones.toArray(Predicate[]::new));
        };
    }
}
