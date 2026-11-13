package cl.valdora.api.solicitud;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "Cambio de estado de una solicitud")
public record CambioEstadoEntrada(
        @NotNull(message = "Indica el nuevo estado.") EstadoSolicitud estado,

        @Schema(example = "Técnico asignado para el martes.")
        @Size(max = 300, message = "Máximo 300 caracteres.") String comentario) {
}
