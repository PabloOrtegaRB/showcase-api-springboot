package cl.valdora.api.solicitud;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class EstadoSolicitudTest {

    @ParameterizedTest(name = "{0} → {1}: {2}")
    @CsvSource({
            "RECIBIDA, EN_PROCESO, true",
            "RECIBIDA, CANCELADA, true",
            "RECIBIDA, COMPLETADA, false",
            "EN_PROCESO, COMPLETADA, true",
            "EN_PROCESO, CANCELADA, true",
            "EN_PROCESO, RECIBIDA, false",
            "COMPLETADA, CANCELADA, false",
            "CANCELADA, EN_PROCESO, false"})
    void respetaElCicloDeVida(EstadoSolicitud origen, EstadoSolicitud destino, boolean permitido) {
        assertThat(origen.puedeCambiarA(destino)).isEqualTo(permitido);
    }

    @Test
    void losEstadosFinalesNoTienenSiguientes() {
        assertThat(EstadoSolicitud.COMPLETADA.siguientes()).isEmpty();
        assertThat(EstadoSolicitud.CANCELADA.siguientes()).isEmpty();
        assertThat(EstadoSolicitud.RECIBIDA.puedeCambiarA(null)).isFalse();
    }
}
