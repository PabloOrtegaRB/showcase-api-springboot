package cl.valdora.api.solicitud;

import cl.valdora.api.cliente.Cliente;
import cl.valdora.api.cliente.ClienteRepository;
import cl.valdora.api.comun.Excepciones.Conflicto;
import cl.valdora.api.comun.Excepciones.RecursoNoEncontrado;
import cl.valdora.api.comun.Pagina;
import cl.valdora.api.integracion.NotificadorExterno;
import cl.valdora.api.producto.Producto;
import cl.valdora.api.producto.ProductoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@Transactional(readOnly = true)
public class SolicitudService {

    private static final Logger LOG = LoggerFactory.getLogger(SolicitudService.class);

    private final SolicitudRepository solicitudes;
    private final ClienteRepository clientes;
    private final ProductoRepository productos;
    private final NotificadorExterno notificador;
    private final Clock reloj;

    public SolicitudService(SolicitudRepository solicitudes, ClienteRepository clientes, ProductoRepository productos,
                            NotificadorExterno notificador, Clock reloj) {
        this.solicitudes = solicitudes;
        this.clientes = clientes;
        this.productos = productos;
        this.notificador = notificador;
        this.reloj = reloj;
    }

    public Pagina<SolicitudSalida> buscar(EstadoSolicitud estado, Prioridad prioridad, Long clienteId,
                                          LocalDate desde, LocalDate hasta, int pagina, int tamano) {
        if (desde != null && hasta != null && desde.isAfter(hasta)) {
            throw new Conflicto("La fecha «desde» no puede ser posterior a «hasta».");
        }
        PageRequest solicitud = PageRequest.of(pagina, tamano, Sort.by("creadaEn").descending().and(Sort.by("id").descending()));
        return Pagina.desde(solicitudes.findAll(SolicitudRepository.filtro(estado, prioridad, clienteId, desde, hasta), solicitud),
                SolicitudSalida::desde);
    }

    public SolicitudSalida obtener(Long id) {
        return SolicitudSalida.desde(buscarEntidad(id));
    }

    @Transactional
    public SolicitudSalida crear(SolicitudEntrada entrada) {
        Cliente cliente = clientes.findById(entrada.clienteId())
                .orElseThrow(() -> new RecursoNoEncontrado("No existe el cliente " + entrada.clienteId() + "."));
        if (!cliente.isActivo()) {
            throw new Conflicto("El cliente está inactivo: actívalo antes de registrar solicitudes.");
        }
        Producto producto = null;
        if (entrada.productoId() != null) {
            producto = productos.findById(entrada.productoId())
                    .orElseThrow(() -> new RecursoNoEncontrado("No existe el producto " + entrada.productoId() + "."));
        }
        LocalDateTime ahora = LocalDateTime.now(reloj);
        SolicitudServicio solicitud = new SolicitudServicio();
        solicitud.setCliente(cliente);
        solicitud.setProducto(producto);
        solicitud.setTitulo(entrada.titulo().trim());
        solicitud.setDescripcion(entrada.descripcion());
        solicitud.setPrioridad(entrada.prioridad());
        solicitud.setEstado(EstadoSolicitud.RECIBIDA);
        solicitud.setCreadaEn(ahora);
        solicitud.setActualizadaEn(ahora);
        SolicitudServicio guardada = solicitudes.save(solicitud);
        notificar(guardada);
        return SolicitudSalida.desde(guardada);
    }

    /** Valida la transición de estado, la guarda y avisa al sistema externo. */
    @Transactional
    public SolicitudSalida cambiarEstado(Long id, CambioEstadoEntrada entrada) {
        SolicitudServicio solicitud = buscarEntidad(id);
        EstadoSolicitud actual = solicitud.getEstado();
        if (!actual.puedeCambiarA(entrada.estado())) {
            throw new Conflicto("No se puede pasar de " + actual + " a " + entrada.estado()
                    + ". Estados permitidos: " + (actual.siguientes().isEmpty() ? "ninguno (estado final)" : actual.siguientes()) + ".");
        }
        solicitud.setEstado(entrada.estado());
        solicitud.setComentarioEstado(entrada.comentario());
        solicitud.setActualizadaEn(LocalDateTime.now(reloj));
        SolicitudServicio guardada = solicitudes.save(solicitud);
        notificar(guardada);
        return SolicitudSalida.desde(guardada);
    }

    private SolicitudServicio buscarEntidad(Long id) {
        return solicitudes.findById(id).orElseThrow(() -> new RecursoNoEncontrado("No existe la solicitud " + id + "."));
    }

    /** Si el sistema externo falla, la operación principal no se revierte: se registra el problema. */
    private void notificar(SolicitudServicio solicitud) {
        try {
            notificador.solicitudActualizada(solicitud.getId(), solicitud.getEstado(), solicitud.getCliente().getEmail());
        } catch (RuntimeException e) {
            LOG.warn("No se pudo avisar al sistema externo sobre la solicitud {}: {}", solicitud.getId(), e.getMessage());
        }
    }
}
