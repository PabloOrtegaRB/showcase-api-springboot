package cl.valdora.api.solicitud;

import cl.valdora.api.cliente.Cliente;
import cl.valdora.api.cliente.ClienteRepository;
import cl.valdora.api.comun.Excepciones.Conflicto;
import cl.valdora.api.comun.Excepciones.RecursoNoEncontrado;
import cl.valdora.api.integracion.NotificadorExterno;
import cl.valdora.api.producto.ProductoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Pruebas unitarias del servicio. Mockito aísla los repositorios y el sistema externo
 * (NotificadorExterno); el reloj se fija para que las fechas sean deterministas.
 */
@ExtendWith(MockitoExtension.class)
class SolicitudServiceTest {

    private static final ZoneId ZONA = ZoneId.of("America/Santiago");
    private static final Clock RELOJ = Clock.fixed(Instant.parse("2026-11-12T15:00:00Z"), ZONA);
    private static final LocalDateTime AHORA = LocalDateTime.now(RELOJ);

    @Mock
    private SolicitudRepository solicitudes;
    @Mock
    private ClienteRepository clientes;
    @Mock
    private ProductoRepository productos;
    @Mock
    private NotificadorExterno notificador;

    private SolicitudService servicio;

    @BeforeEach
    void preparar() {
        servicio = new SolicitudService(solicitudes, clientes, productos, notificador, RELOJ);
    }

    private static Cliente cliente(boolean activo) {
        Cliente c = new Cliente();
        c.setId(1L);
        c.setRazonSocial("Bodegas Arrayán Sur SpA");
        c.setEmail("contacto@cliente1.example");
        c.setActivo(activo);
        return c;
    }

    private static SolicitudServicio solicitudEn(EstadoSolicitud estado) {
        SolicitudServicio s = new SolicitudServicio();
        s.setId(10L);
        s.setCliente(cliente(true));
        s.setTitulo("Mantención preventiva");
        s.setPrioridad(Prioridad.ALTA);
        s.setEstado(estado);
        s.setCreadaEn(AHORA.minusDays(2));
        s.setActualizadaEn(AHORA.minusDays(2));
        return s;
    }

    private static SolicitudEntrada entrada() {
        return new SolicitudEntrada(1L, null, "  Mantención preventiva  ", "Planta 2", Prioridad.ALTA);
    }

    @Test
    void crearConClienteInexistenteNoGuardaNiNotifica() {
        when(clientes.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.crear(entrada())).isInstanceOf(RecursoNoEncontrado.class);
        verify(solicitudes, never()).save(any());
        verifyNoInteractions(notificador);
    }

    @Test
    void crearConClienteInactivoEsUnConflicto() {
        when(clientes.findById(1L)).thenReturn(Optional.of(cliente(false)));

        assertThatThrownBy(() -> servicio.crear(entrada()))
                .isInstanceOf(Conflicto.class)
                .hasMessageContaining("inactivo");
        verify(solicitudes, never()).save(any());
    }

    @Test
    void crearGuardaComoRecibidaYAvisaAlSistemaExterno() {
        when(clientes.findById(1L)).thenReturn(Optional.of(cliente(true)));
        when(solicitudes.save(any(SolicitudServicio.class))).thenAnswer(inv -> {
            SolicitudServicio s = inv.getArgument(0);
            s.setId(10L);
            return s;
        });

        SolicitudSalida creada = servicio.crear(entrada());

        ArgumentCaptor<SolicitudServicio> guardada = ArgumentCaptor.forClass(SolicitudServicio.class);
        verify(solicitudes).save(guardada.capture());
        assertThat(guardada.getValue().getTitulo()).isEqualTo("Mantención preventiva");
        assertThat(creada.estado()).isEqualTo(EstadoSolicitud.RECIBIDA);
        assertThat(creada.creadaEn()).isEqualTo(AHORA);
        assertThat(creada.estadosSiguientes()).containsExactlyInAnyOrder(EstadoSolicitud.EN_PROCESO, EstadoSolicitud.CANCELADA);
        verify(notificador).solicitudActualizada(10L, EstadoSolicitud.RECIBIDA, "contacto@cliente1.example");
    }

    @Test
    void cambiarAUnEstadoPermitidoActualizaYNotifica() {
        SolicitudServicio solicitud = solicitudEn(EstadoSolicitud.RECIBIDA);
        when(solicitudes.findById(10L)).thenReturn(Optional.of(solicitud));
        when(solicitudes.save(solicitud)).thenReturn(solicitud);

        SolicitudSalida resultado = servicio.cambiarEstado(10L, new CambioEstadoEntrada(EstadoSolicitud.EN_PROCESO, "Técnico asignado"));

        assertThat(resultado.estado()).isEqualTo(EstadoSolicitud.EN_PROCESO);
        assertThat(resultado.comentarioEstado()).isEqualTo("Técnico asignado");
        assertThat(resultado.actualizadaEn()).isEqualTo(AHORA);
        verify(notificador).solicitudActualizada(10L, EstadoSolicitud.EN_PROCESO, "contacto@cliente1.example");
    }

    @Test
    void unaTransicionNoPermitidaEsUnConflictoYNoNotifica() {
        when(solicitudes.findById(10L)).thenReturn(Optional.of(solicitudEn(EstadoSolicitud.COMPLETADA)));

        assertThatThrownBy(() -> servicio.cambiarEstado(10L, new CambioEstadoEntrada(EstadoSolicitud.EN_PROCESO, null)))
                .isInstanceOf(Conflicto.class)
                .hasMessageContaining("No se puede pasar de COMPLETADA a EN_PROCESO");
        verify(solicitudes, never()).save(any());
        verifyNoInteractions(notificador);
    }

    @Test
    void siElSistemaExternoFallaElCambioIgualSeGuarda() {
        SolicitudServicio solicitud = solicitudEn(EstadoSolicitud.EN_PROCESO);
        when(solicitudes.findById(10L)).thenReturn(Optional.of(solicitud));
        when(solicitudes.save(solicitud)).thenReturn(solicitud);
        doThrow(new IllegalStateException("servicio externo caído"))
                .when(notificador).solicitudActualizada(any(), any(), any());

        SolicitudSalida resultado = servicio.cambiarEstado(10L, new CambioEstadoEntrada(EstadoSolicitud.COMPLETADA, "Listo"));

        assertThat(resultado.estado()).isEqualTo(EstadoSolicitud.COMPLETADA);
        verify(solicitudes).save(solicitud);
    }

    @Test
    void unRangoDeFechasInvertidoEsRechazado() {
        assertThatThrownBy(() -> servicio.buscar(null, null, null,
                LocalDate.of(2026, 11, 30), LocalDate.of(2026, 11, 1), 0, 10))
                .isInstanceOf(Conflicto.class);
        verifyNoInteractions(solicitudes);
    }
}
