package cl.valdora.api.solicitud;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Ciclo de vida de una solicitud de servicio:
 * RECIBIDA → EN_PROCESO → COMPLETADA, y desde RECIBIDA o EN_PROCESO se puede CANCELAR.
 */
public enum EstadoSolicitud {
    RECIBIDA, EN_PROCESO, COMPLETADA, CANCELADA;

    private static final Map<EstadoSolicitud, Set<EstadoSolicitud>> TRANSICIONES = Map.of(
            RECIBIDA, EnumSet.of(EN_PROCESO, CANCELADA),
            EN_PROCESO, EnumSet.of(COMPLETADA, CANCELADA),
            COMPLETADA, EnumSet.noneOf(EstadoSolicitud.class),
            CANCELADA, EnumSet.noneOf(EstadoSolicitud.class));

    public boolean puedeCambiarA(EstadoSolicitud destino) {
        return destino != null && TRANSICIONES.get(this).contains(destino);
    }

    public Set<EstadoSolicitud> siguientes() {
        return Collections.unmodifiableSet(TRANSICIONES.get(this));
    }
}
