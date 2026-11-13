package cl.valdora.api.solicitud;

import cl.valdora.api.cliente.Cliente;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/** Repositorio contra H2 real: filtros, rango de fechas y carga anticipada con EntityGraph. */
@DataJpaTest
class SolicitudRepositoryTest {

    @Autowired
    private TestEntityManager em;

    @Autowired
    private SolicitudRepository repositorio;

    private Cliente uno;

    private Cliente cliente(String nombre, String rut) {
        Cliente c = new Cliente();
        c.setRut(rut);
        c.setRazonSocial(nombre);
        c.setEmail(rut + "@demo.example");
        c.setComuna("Santiago");
        c.setFechaRegistro(LocalDate.of(2026, 1, 1));
        return em.persist(c);
    }

    private void solicitud(Cliente cliente, EstadoSolicitud estado, Prioridad prioridad, LocalDateTime creada) {
        SolicitudServicio s = new SolicitudServicio();
        s.setCliente(cliente);
        s.setTitulo("Solicitud " + estado);
        s.setPrioridad(prioridad);
        s.setEstado(estado);
        s.setCreadaEn(creada);
        s.setActualizadaEn(creada);
        em.persist(s);
    }

    @BeforeEach
    void cargarDatos() {
        uno = cliente("Cliente Uno SpA", "11111111-1");
        Cliente dos = cliente("Cliente Dos SpA", "22222222-2");
        solicitud(uno, EstadoSolicitud.RECIBIDA, Prioridad.ALTA, LocalDateTime.of(2026, 11, 1, 9, 0));
        solicitud(uno, EstadoSolicitud.EN_PROCESO, Prioridad.MEDIA, LocalDateTime.of(2026, 11, 5, 23, 59));
        solicitud(dos, EstadoSolicitud.RECIBIDA, Prioridad.CRITICA, LocalDateTime.of(2026, 11, 10, 8, 30));
        em.flush();
        em.clear();
    }

    @Test
    void filtraPorEstadoYCliente() {
        Page<SolicitudServicio> resultado = repositorio.findAll(
                SolicitudRepository.filtro(EstadoSolicitud.RECIBIDA, null, uno.getId(), null, null), PageRequest.of(0, 10));
        assertThat(resultado.getTotalElements()).isEqualTo(1);
        assertThat(resultado.getContent().get(0).getPrioridad()).isEqualTo(Prioridad.ALTA);
    }

    @Test
    void elRangoDeFechasIncluyeElDiaCompletoDeHasta() {
        Page<SolicitudServicio> resultado = repositorio.findAll(
                SolicitudRepository.filtro(null, null, null, LocalDate.of(2026, 11, 1), LocalDate.of(2026, 11, 5)),
                PageRequest.of(0, 10));
        assertThat(resultado.getTotalElements()).isEqualTo(2);
    }

    @Test
    void cargaClienteYProductoEnLaMismaConsulta() {
        Page<SolicitudServicio> resultado = repositorio.findAll(
                SolicitudRepository.filtro(null, Prioridad.CRITICA, null, null, null), PageRequest.of(0, 10));
        SolicitudServicio solicitud = resultado.getContent().get(0);
        boolean clienteCargado = em.getEntityManager().getEntityManagerFactory()
                .getPersistenceUnitUtil().isLoaded(solicitud, "cliente");
        assertThat(clienteCargado).isTrue();
        assertThat(solicitud.getCliente().getRazonSocial()).isEqualTo("Cliente Dos SpA");
    }
}
