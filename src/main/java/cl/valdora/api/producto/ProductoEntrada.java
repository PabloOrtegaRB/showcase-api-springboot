package cl.valdora.api.producto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

@Schema(description = "Datos para crear o actualizar un producto")
public record ProductoEntrada(
        @Schema(example = "REP-1001")
        @NotBlank(message = "Ingresa el SKU.") @Pattern(regexp = "^[A-Za-z]{3}-\\d{4}$", message = "Usa el formato ABC-1234.") String sku,

        @Schema(example = "Filtro de aire 24x24 pulgadas")
        @NotBlank(message = "Ingresa el nombre.") @Size(max = 120, message = "Máximo 120 caracteres.") String nombre,

        @NotNull(message = "Indica la categoría.") Categoria categoria,

        @Schema(description = "Precio en pesos chilenos, sin decimales", example = "12990")
        @NotNull(message = "Ingresa el precio.") @DecimalMin(value = "1", message = "El precio debe ser mayor que cero.")
        @Digits(integer = 10, fraction = 0, message = "El precio va en pesos, sin decimales.") BigDecimal precio,

        @Schema(example = "48")
        @NotNull(message = "Ingresa el stock.") @Min(value = 0, message = "El stock no puede ser negativo.") Integer stock,

        @Schema(description = "Si no se indica, el producto queda activo", example = "true")
        Boolean activo) {
}
