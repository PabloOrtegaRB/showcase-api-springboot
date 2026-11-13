package cl.valdora.api.cliente;

import cl.valdora.api.comun.Pagina;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@RequestMapping("/api/v1/clientes")
@Tag(name = "Clientes", description = "Empresas que solicitan servicios (datos ficticios)")
public class ClienteController {

    private final ClienteService servicio;

    public ClienteController(ClienteService servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    @Operation(summary = "Lista clientes con filtros y paginación")
    public Pagina<ClienteSalida> listar(
            @Parameter(description = "Texto en razón social, correo o RUT") @RequestParam(required = false) String q,
            @Parameter(description = "Comuna exacta") @RequestParam(required = false) String comuna,
            @Parameter(description = "true: solo activos · false: solo inactivos") @RequestParam(required = false) Boolean activo,
            @Parameter(description = "Página, desde 0") @RequestParam(defaultValue = "0") @Min(0) int pagina,
            @Parameter(description = "Elementos por página (1 a 100)") @RequestParam(defaultValue = "10") @Min(1) @Max(100) int tamano) {
        return servicio.buscar(q, comuna, activo, pagina, tamano);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtiene un cliente")
    public ClienteSalida obtener(@PathVariable Long id) {
        return servicio.obtener(id);
    }

    @PostMapping
    @Operation(summary = "Crea un cliente (valida el RUT y que no esté repetido)")
    public ResponseEntity<ClienteSalida> crear(@Valid @RequestBody ClienteEntrada entrada) {
        ClienteSalida creado = servicio.crear(entrada);
        return ResponseEntity.created(URI.create("/api/v1/clientes/" + creado.id())).body(creado);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualiza un cliente")
    public ClienteSalida actualizar(@PathVariable Long id, @Valid @RequestBody ClienteEntrada entrada) {
        return servicio.actualizar(id, entrada);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Elimina un cliente sin solicitudes asociadas")
    public void eliminar(@PathVariable Long id) {
        servicio.eliminar(id);
    }
}
