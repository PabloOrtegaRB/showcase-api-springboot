package cl.valdora.api.solicitud;

import cl.valdora.api.comun.Pagina;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/solicitudes")
@Tag(name = "Solicitudes", description = "Solicitudes de servicio y su ciclo de vida (datos ficticios)")
public class SolicitudController {

    private final SolicitudService servicio;

    public SolicitudController(SolicitudService servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    @Operation(summary = "Lista solicitudes con filtros y paginación (más recientes primero)")
    public Pagina<SolicitudSalida> listar(
            @RequestParam(required = false) EstadoSolicitud estado,
            @RequestParam(required = false) Prioridad prioridad,
            @Parameter(description = "Id del cliente") @RequestParam(required = false) Long clienteId,
            @Parameter(description = "Creadas desde esta fecha (AAAA-MM-DD, inclusive)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @Parameter(description = "Creadas hasta esta fecha (AAAA-MM-DD, inclusive)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @Parameter(description = "Página, desde 0") @RequestParam(defaultValue = "0") @Min(0) int pagina,
            @Parameter(description = "Elementos por página (1 a 100)") @RequestParam(defaultValue = "10") @Min(1) @Max(100) int tamano) {
        return servicio.buscar(estado, prioridad, clienteId, desde, hasta, pagina, tamano);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtiene una solicitud")
    public SolicitudSalida obtener(@PathVariable Long id) {
        return servicio.obtener(id);
    }

    @PostMapping
    @Operation(summary = "Registra una solicitud (el cliente debe estar activo)")
    public ResponseEntity<SolicitudSalida> crear(@Valid @RequestBody SolicitudEntrada entrada) {
        SolicitudSalida creada = servicio.crear(entrada);
        return ResponseEntity.created(URI.create("/api/v1/solicitudes/" + creada.id())).body(creada);
    }

    @PatchMapping("/{id}/estado")
    @Operation(summary = "Cambia el estado de una solicitud (409 si la transición no está permitida)")
    public SolicitudSalida cambiarEstado(@PathVariable Long id, @Valid @RequestBody CambioEstadoEntrada entrada) {
        return servicio.cambiarEstado(id, entrada);
    }
}
