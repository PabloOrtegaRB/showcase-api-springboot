package cl.valdora.api.cliente;

import java.time.LocalDate;

public record ClienteSalida(Long id, String rut, String razonSocial, String email, String telefono,
                            String comuna, boolean activo, LocalDate fechaRegistro) {

    public static ClienteSalida desde(Cliente c) {
        return new ClienteSalida(c.getId(), c.getRut(), c.getRazonSocial(), c.getEmail(), c.getTelefono(),
                c.getComuna(), c.isActivo(), c.getFechaRegistro());
    }
}
