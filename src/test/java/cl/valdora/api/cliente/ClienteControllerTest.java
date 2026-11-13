package cl.valdora.api.cliente;

import cl.valdora.api.comun.Excepciones.Conflicto;
import cl.valdora.api.comun.Excepciones.RecursoNoEncontrado;
import cl.valdora.api.comun.Pagina;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Capa web aislada: el servicio se reemplaza con @MockitoBean. */
@WebMvcTest(ClienteController.class)
class ClienteControllerTest {

    private static final String VALIDO = """
            {"rut":"12.345.678-5","razonSocial":"Comercial Demo SpA","email":"contacto@demo.example",
             "telefono":"+56 9 1234 5678","comuna":"Santiago"}""";

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private ClienteService servicio;

    private static ClienteSalida salida() {
        return new ClienteSalida(7L, "12345678-5", "Comercial Demo SpA", "contacto@demo.example",
                "+56 9 1234 5678", "Santiago", true, LocalDate.of(2026, 11, 12));
    }

    @Test
    void crearDevuelve201ConLocation() throws Exception {
        when(servicio.crear(any(ClienteEntrada.class))).thenReturn(salida());

        mvc.perform(post("/api/v1/clientes").contentType(MediaType.APPLICATION_JSON).content(VALIDO))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/v1/clientes/7"))
                .andExpect(jsonPath("$.id").value(7))
                .andExpect(jsonPath("$.razonSocial").value("Comercial Demo SpA"));
    }

    @Test
    void datosInvalidosResponden400EnFormatoProblemDetails() throws Exception {
        String invalido = """
                {"rut":"12.345.678-9","razonSocial":"","email":"no-es-correo","comuna":"Santiago"}""";

        mvc.perform(post("/api/v1/clientes").contentType(MediaType.APPLICATION_JSON).content(invalido))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Datos inválidos"))
                .andExpect(jsonPath("$.errores[?(@.campo == 'rut')].mensaje").value("El RUT no es válido: revisa el dígito verificador."))
                .andExpect(jsonPath("$.errores[?(@.campo == 'email')]").exists())
                .andExpect(jsonPath("$.errores[?(@.campo == 'razonSocial')]").exists());
        verifyNoInteractions(servicio);
    }

    @Test
    void unJsonMalFormadoResponde400() throws Exception {
        mvc.perform(post("/api/v1/clientes").contentType(MediaType.APPLICATION_JSON).content("{\"rut\": "))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(servicio);
    }

    @Test
    void unClienteInexistenteResponde404() throws Exception {
        when(servicio.obtener(99L)).thenThrow(new RecursoNoEncontrado("No existe el cliente 99."));

        mvc.perform(get("/api/v1/clientes/99"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Recurso no encontrado"))
                .andExpect(jsonPath("$.detail").value("No existe el cliente 99."));
    }

    @Test
    void unRutRepetidoResponde409() throws Exception {
        when(servicio.crear(any(ClienteEntrada.class))).thenThrow(new Conflicto("Ya existe un cliente con el RUT 12345678-5."));

        mvc.perform(post("/api/v1/clientes").contentType(MediaType.APPLICATION_JSON).content(VALIDO))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    void listarDevuelveUnaPaginaEstable() throws Exception {
        when(servicio.buscar("demo", null, true, 0, 10)).thenReturn(new Pagina<>(List.of(salida()), 0, 10, 1, 1));

        mvc.perform(get("/api/v1/clientes").param("q", "demo").param("activo", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido[0].rut").value("12345678-5"))
                .andExpect(jsonPath("$.totalElementos").value(1))
                .andExpect(jsonPath("$.totalPaginas").value(1));
    }

    @Test
    void unTamanoDePaginaFueraDeRangoResponde400() throws Exception {
        mvc.perform(get("/api/v1/clientes").param("tamano", "500"))
                .andExpect(status().isBadRequest());
        verifyNoInteractions(servicio);
    }
}
