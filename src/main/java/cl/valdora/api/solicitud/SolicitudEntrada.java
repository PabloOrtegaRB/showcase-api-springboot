package cl.valdora.api.solicitud;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "Datos para registrar una solicitud de servicio")
public record SolicitudEntrada(
        @Schema(example = "1")
        @NotNull(message = "Indica el cliente.") Long clienteId,

        @Schema(description = "Producto relacionado (opcional)", example = "3")
        Long productoId,

        @Schema(example = "Mantención preventiva de chiller")
        @NotBlank(message = "Ingresa un título.") @Size(max = 120, message = "Máximo 120 caracteres.") String titulo,

        @Schema(example = "Revisión trimestral del equipo de la planta 2.")
        @Size(max = 1000, message = "Máximo 1000 caracteres.") String descripcion,

        @NotNull(message = "Indica la prioridad.") Prioridad prioridad) {
}
