package cl.valdora.api.integracion;

import cl.valdora.api.solicitud.EstadoSolicitud;

/**
 * Puerto hacia un sistema externo que debe enterarse de cada cambio en una solicitud
 * (por ejemplo, un servicio de notificaciones o un CRM). Permite aislarlo en las pruebas.
 */
public interface NotificadorExterno {

    void solicitudActualizada(Long idSolicitud, EstadoSolicitud estado, String correoCliente);
}
