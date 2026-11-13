package cl.valdora.api.integracion;

import cl.valdora.api.solicitud.EstadoSolicitud;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Implementación SIMULADA del sistema externo: solo deja un registro en el log.
 * No envía datos a ningún servicio ni requiere credenciales.
 */
@Component
public class NotificadorRegistro implements NotificadorExterno {

    private static final Logger LOG = LoggerFactory.getLogger(NotificadorRegistro.class);

    @Override
    public void solicitudActualizada(Long idSolicitud, EstadoSolicitud estado, String correoCliente) {
        LOG.info("[Simulación] Aviso al sistema externo: solicitud {} en estado {} (destinatario {})",
                idSolicitud, estado, enmascarar(correoCliente));
    }

    /** Nunca se registra un correo completo en los logs. */
    static String enmascarar(String correo) {
        if (correo == null || !correo.contains("@")) {
            return "(sin correo)";
        }
        int arroba = correo.indexOf('@');
        return correo.charAt(0) + "***" + correo.substring(arroba);
    }
}
