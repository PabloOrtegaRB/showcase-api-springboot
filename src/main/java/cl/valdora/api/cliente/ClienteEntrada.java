package cl.valdora.api.cliente;

import cl.valdora.api.comun.RutValido;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "Datos para crear o actualizar un cliente")
public record ClienteEntrada(
        @Schema(example = "76.100.000-4")
        @NotBlank(message = "Ingresa el RUT.") @RutValido String rut,

        @Schema(example = "Bodegas Arrayán Sur SpA")
        @NotBlank(message = "Ingresa la razón social.") @Size(max = 120, message = "Máximo 120 caracteres.") String razonSocial,

        @Schema(example = "contacto@arrayan-sur.example")
        @NotBlank(message = "Ingresa el correo.") @Email(message = "Revisa el formato del correo.")
        @Size(max = 120, message = "Máximo 120 caracteres.") String email,

        @Schema(example = "+56 9 5555 0100")
        @Pattern(regexp = "^$|^\\+?56\\s?9\\s?\\d{4}\\s?\\d{4}$", message = "Usa el formato +56 9 1234 5678.") String telefono,

        @Schema(example = "Santiago")
        @NotBlank(message = "Ingresa la comuna.") @Size(max = 40, message = "Máximo 40 caracteres.") String comuna,

        @Schema(description = "Si no se indica, el cliente queda activo", example = "true")
        Boolean activo) {
}
