package cl.valdora.api;

import cl.valdora.api.cliente.ClienteRepository;
import cl.valdora.api.comun.Rut;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Verifica que los datos ficticios de demostración se carguen completos y sean válidos. */
@SpringBootTest(properties = "app.datos-demo=true")
@AutoConfigureMockMvc
class DatosDemostracionTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ClienteRepository clientes;

    @Test
    void cargaClientesProductosYSolicitudesDeEjemplo() throws Exception {
        mvc.perform(get("/api/v1/clientes")).andExpect(status().isOk()).andExpect(jsonPath("$.totalElementos").value(8));
        mvc.perform(get("/api/v1/productos")).andExpect(status().isOk()).andExpect(jsonPath("$.totalElementos").value(10));
        mvc.perform(get("/api/v1/solicitudes")).andExpect(status().isOk()).andExpect(jsonPath("$.totalElementos").value(12));
        mvc.perform(get("/api/v1/clientes").param("activo", "false")).andExpect(jsonPath("$.totalElementos").value(1));
    }

    @Test
    void todosLosRutDeEjemploSonValidos() {
        assertThat(clientes.findAll()).isNotEmpty()
                .allSatisfy(c -> assertThat(Rut.esValido(c.getRut())).as(c.getRut()).isTrue());
    }
}
