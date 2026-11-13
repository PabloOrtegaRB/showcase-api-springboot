package cl.valdora.api.integracion;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class NotificadorRegistroTest {

    @Test
    void nuncaRegistraElCorreoCompleto() {
        assertThat(NotificadorRegistro.enmascarar("contacto@cliente1.example")).isEqualTo("c***@cliente1.example");
        assertThat(NotificadorRegistro.enmascarar(null)).isEqualTo("(sin correo)");
        assertThat(NotificadorRegistro.enmascarar("sin-arroba")).isEqualTo("(sin correo)");
    }
}
