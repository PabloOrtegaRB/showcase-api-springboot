package cl.valdora.api;

import cl.valdora.api.comun.Rut;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Pruebas de integración reales: aplicación completa + H2, sin simulaciones.
 * Cada prueba usa RUT y SKU propios para no depender del orden de ejecución.
 */
@SpringBootTest(properties = "app.datos-demo=false")
@AutoConfigureMockMvc
class ApiIntegracionTest {

    @Autowired
    private MockMvc mvc;

    private static String rut(int cuerpo) {
        return cuerpo + "-" + Rut.calcularDv(cuerpo);
    }

    private long crearCliente(int cuerpoRut, String nombre) throws Exception {
        String json = """
                {"rut":"%s","razonSocial":"%s","email":"contacto@%d.example","comuna":"Santiago"}"""
                .formatted(rut(cuerpoRut), nombre, cuerpoRut);
        String respuesta = mvc.perform(post("/api/v1/clientes").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(respuesta, "$.id")).longValue();
    }

    private long crearProducto(String sku) throws Exception {
        String json = """
                {"sku":"%s","nombre":"Producto %s","categoria":"REPUESTOS","precio":12990,"stock":5}""".formatted(sku, sku);
        String respuesta = mvc.perform(post("/api/v1/productos").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(respuesta, "$.id")).longValue();
    }

    private long crearSolicitud(long clienteId, long productoId) throws Exception {
        String json = """
                {"clienteId":%d,"productoId":%d,"titulo":"Mantención preventiva","prioridad":"ALTA"}"""
                .formatted(clienteId, productoId);
        String respuesta = mvc.perform(post("/api/v1/solicitudes").contentType(MediaType.APPLICATION_JSON).content(json))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value("RECIBIDA"))
                .andExpect(jsonPath("$.estadosSiguientes", hasSize(2)))
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(respuesta, "$.id")).longValue();
    }

    private void cambiarEstado(long id, String estado, int esperado) throws Exception {
        mvc.perform(patch("/api/v1/solicitudes/{id}/estado", id).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"estado\":\"" + estado + "\",\"comentario\":\"Prueba de integración\"}"))
                .andExpect(status().is(esperado));
    }

    @Test
    void cicloDeVidaCompletoDeUnaSolicitud() throws Exception {
        long cliente = crearCliente(15_000_001, "Integración Ciclo SpA");
        long producto = crearProducto("INT-0001");
        long solicitud = crearSolicitud(cliente, producto);

        cambiarEstado(solicitud, "EN_PROCESO", 200);
        cambiarEstado(solicitud, "COMPLETADA", 200);
        mvc.perform(patch("/api/v1/solicitudes/{id}/estado", solicitud).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"estado\":\"CANCELADA\"}"))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value(containsString("estado final")));

        mvc.perform(get("/api/v1/solicitudes").param("estado", "COMPLETADA").param("clienteId", String.valueOf(cliente)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElementos").value(1))
                .andExpect(jsonPath("$.contenido[0].producto.sku").value("INT-0001"))
                .andExpect(jsonPath("$.contenido[0].cliente.razonSocial").value("Integración Ciclo SpA"));
    }

    @Test
    void unClienteConSolicitudesNoSeEliminaYUnoSinSolicitudesSi() throws Exception {
        long conSolicitud = crearCliente(15_000_002, "Integración Con Historial SpA");
        crearSolicitud(conSolicitud, crearProducto("INT-0002"));
        mvc.perform(delete("/api/v1/clientes/{id}", conSolicitud)).andExpect(status().isConflict());

        long sinSolicitud = crearCliente(15_000_003, "Integración Sin Historial SpA");
        mvc.perform(delete("/api/v1/clientes/{id}", sinSolicitud)).andExpect(status().isNoContent());
        mvc.perform(get("/api/v1/clientes/{id}", sinSolicitud)).andExpect(status().isNotFound());
    }

    @Test
    void unRutRepetidoResponde409AunqueSeEscribaConPuntos() throws Exception {
        crearCliente(15_000_004, "Integración RUT SpA");
        String conPuntos = "15.000.004-" + Rut.calcularDv(15_000_004);
        mvc.perform(post("/api/v1/clientes").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rut\":\"" + conPuntos + "\",\"razonSocial\":\"Otra SpA\",\"email\":\"a@b.example\",\"comuna\":\"Maipú\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void lasSolicitudesExigenUnClienteActivo() throws Exception {
        long cliente = crearCliente(15_000_005, "Integración Inactivo SpA");
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put("/api/v1/clientes/{id}", cliente)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"rut\":\"" + rut(15_000_005) + "\",\"razonSocial\":\"Integración Inactivo SpA\","
                                + "\"email\":\"contacto@15000005.example\",\"comuna\":\"Santiago\",\"activo\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activo").value(false));
        mvc.perform(post("/api/v1/solicitudes").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"clienteId\":" + cliente + ",\"titulo\":\"No debería crearse\",\"prioridad\":\"BAJA\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void paginaYFiltraClientes() throws Exception {
        crearCliente(15_000_011, "Paginación Alfa SpA");
        crearCliente(15_000_012, "Paginación Beta SpA");
        crearCliente(15_000_013, "Paginación Gamma SpA");

        mvc.perform(get("/api/v1/clientes").param("q", "paginación").param("tamano", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contenido", hasSize(2)))
                .andExpect(jsonPath("$.contenido[0].razonSocial").value("Paginación Alfa SpA"))
                .andExpect(jsonPath("$.totalElementos").value(3))
                .andExpect(jsonPath("$.totalPaginas").value(2));
        mvc.perform(get("/api/v1/clientes").param("q", "paginación").param("tamano", "2").param("pagina", "1"))
                .andExpect(jsonPath("$.contenido", hasSize(1)))
                .andExpect(jsonPath("$.contenido[0].razonSocial").value("Paginación Gamma SpA"));
    }

    @Test
    void productosSeFiltranActualizanYProtegenSuHistorial() throws Exception {
        long conHistorial = crearProducto("FIL-0001");
        long sinHistorial = crearProducto("FIL-0002");
        crearSolicitud(crearCliente(15_000_021, "Integración Productos SpA"), conHistorial);

        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put("/api/v1/productos/{id}", sinHistorial)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sku\":\"fil-0002\",\"nombre\":\"Termostato de prueba\",\"categoria\":\"CLIMATIZACION\","
                                + "\"precio\":54990,\"stock\":3,\"activo\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sku").value("FIL-0002"))
                .andExpect(jsonPath("$.activo").value(false));

        mvc.perform(get("/api/v1/productos").param("q", "fil-").param("categoria", "CLIMATIZACION").param("activo", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElementos").value(1))
                .andExpect(jsonPath("$.contenido[0].nombre").value("Termostato de prueba"));

        mvc.perform(post("/api/v1/productos").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sku\":\"FIL-0001\",\"nombre\":\"Repetido\",\"categoria\":\"REPUESTOS\",\"precio\":1000,\"stock\":1}"))
                .andExpect(status().isConflict());
        mvc.perform(delete("/api/v1/productos/{id}", conHistorial)).andExpect(status().isConflict());
        mvc.perform(delete("/api/v1/productos/{id}", sinHistorial)).andExpect(status().isNoContent());
        mvc.perform(get("/api/v1/productos/{id}", sinHistorial)).andExpect(status().isNotFound());
    }

    @Test
    void validacionYRutasDesconocidasUsanProblemDetails() throws Exception {
        mvc.perform(post("/api/v1/productos").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"sku\":\"malo\",\"nombre\":\"\",\"categoria\":\"REPUESTOS\",\"precio\":0,\"stock\":-1}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errores", hasSize(4)));
        mvc.perform(get("/api/v1/solicitudes").param("estado", "INVENTADO"))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/no-existe"))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
    }

    @Test
    void publicaLaDocumentacionOpenApiYElEstadoDeSalud() throws Exception {
        mvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title").value("API Valdora (demostrativa)"))
                .andExpect(content().string(containsString("/api/v1/solicitudes/{id}/estado")));
        mvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }
}
