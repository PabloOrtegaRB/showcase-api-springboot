package cl.valdora.api.producto;

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
@RequestMapping("/api/v1/productos")
@Tag(name = "Productos", description = "Catálogo de repuestos, herramientas y equipos (datos ficticios)")
public class ProductoController {

    private final ProductoService servicio;

    public ProductoController(ProductoService servicio) {
        this.servicio = servicio;
    }

    @GetMapping
    @Operation(summary = "Lista productos con filtros y paginación")
    public Pagina<ProductoSalida> listar(
            @Parameter(description = "Texto en nombre o SKU") @RequestParam(required = false) String q,
            @Parameter(description = "Categoría exacta") @RequestParam(required = false) Categoria categoria,
            @Parameter(description = "true: solo activos · false: solo inactivos") @RequestParam(required = false) Boolean activo,
            @Parameter(description = "Página, desde 0") @RequestParam(defaultValue = "0") @Min(0) int pagina,
            @Parameter(description = "Elementos por página (1 a 100)") @RequestParam(defaultValue = "10") @Min(1) @Max(100) int tamano) {
        return servicio.buscar(q, categoria, activo, pagina, tamano);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtiene un producto")
    public ProductoSalida obtener(@PathVariable Long id) {
        return servicio.obtener(id);
    }

    @PostMapping
    @Operation(summary = "Crea un producto (el SKU no puede repetirse)")
    public ResponseEntity<ProductoSalida> crear(@Valid @RequestBody ProductoEntrada entrada) {
        ProductoSalida creado = servicio.crear(entrada);
        return ResponseEntity.created(URI.create("/api/v1/productos/" + creado.id())).body(creado);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualiza un producto")
    public ProductoSalida actualizar(@PathVariable Long id, @Valid @RequestBody ProductoEntrada entrada) {
        return servicio.actualizar(id, entrada);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Elimina un producto que no esté asociado a solicitudes")
    public void eliminar(@PathVariable Long id) {
        servicio.eliminar(id);
    }
}
