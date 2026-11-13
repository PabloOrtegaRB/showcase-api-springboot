package cl.valdora.api.cliente;

import cl.valdora.api.comun.Excepciones.Conflicto;
import cl.valdora.api.solicitud.SolicitudRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClienteServiceTest {

    private static final Clock RELOJ = Clock.fixed(Instant.parse("2026-11-12T15:00:00Z"), ZoneId.of("America/Santiago"));

    @Mock
    private ClienteRepository clientes;
    @Mock
    private SolicitudRepository solicitudes;

    private ClienteService servicio;

    @BeforeEach
    void preparar() {
        servicio = new ClienteService(clientes, solicitudes, RELOJ);
    }

    private static ClienteEntrada entrada() {
        return new ClienteEntrada("12.345.678-5", " Comercial Demo SpA ", "Contacto@Demo.EXAMPLE", "", "Santiago", null);
    }

    @Test
    void crearNormalizaYQuedaActivoPorDefecto() {
        when(clientes.existsByRut("12345678-5")).thenReturn(false);
        when(clientes.save(any(Cliente.class))).thenAnswer(inv -> inv.getArgument(0));

        ClienteSalida creado = servicio.crear(entrada());

        assertThat(creado.rut()).isEqualTo("12345678-5");
        assertThat(creado.razonSocial()).isEqualTo("Comercial Demo SpA");
        assertThat(creado.email()).isEqualTo("contacto@demo.example");
        assertThat(creado.telefono()).isNull();
        assertThat(creado.activo()).isTrue();
        assertThat(creado.fechaRegistro()).isEqualTo(LocalDate.of(2026, 11, 12));
    }

    @Test
    void unRutRepetidoEsUnConflicto() {
        when(clientes.existsByRut("12345678-5")).thenReturn(true);

        assertThatThrownBy(() -> servicio.crear(entrada())).isInstanceOf(Conflicto.class);
        verify(clientes, never()).save(any());
    }

    @Test
    void noSeEliminaUnClienteConSolicitudes() {
        Cliente cliente = new Cliente();
        cliente.setId(4L);
        when(clientes.findById(4L)).thenReturn(Optional.of(cliente));
        when(solicitudes.existsByClienteId(4L)).thenReturn(true);

        assertThatThrownBy(() -> servicio.eliminar(4L))
                .isInstanceOf(Conflicto.class)
                .hasMessageContaining("desactívalo");
        verify(clientes, never()).delete(any(Cliente.class));
    }
}
