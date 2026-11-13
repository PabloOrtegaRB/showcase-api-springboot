package cl.valdora.api.solicitud;

import java.time.LocalDateTime;
import java.util.Set;

public record SolicitudSalida(Long id, String titulo, String descripcion, Prioridad prioridad,
                              EstadoSolicitud estado, Set<EstadoSolicitud> estadosSiguientes,
                              ClienteResumen cliente, ProductoResumen producto, String comentarioEstado,
                              LocalDateTime creadaEn, LocalDateTime actualizadaEn) {

    public record ClienteResumen(Long id, String razonSocial) {
    }

    public record ProductoResumen(Long id, String sku, String nombre) {
    }

    public static SolicitudSalida desde(SolicitudServicio s) {
        ProductoResumen producto = s.getProducto() == null ? null
                : new ProductoResumen(s.getProducto().getId(), s.getProducto().getSku(), s.getProducto().getNombre());
        return new SolicitudSalida(s.getId(), s.getTitulo(), s.getDescripcion(), s.getPrioridad(), s.getEstado(),
                s.getEstado().siguientes(),
                new ClienteResumen(s.getCliente().getId(), s.getCliente().getRazonSocial()),
                producto, s.getComentarioEstado(), s.getCreadaEn(), s.getActualizadaEn());
    }
}
