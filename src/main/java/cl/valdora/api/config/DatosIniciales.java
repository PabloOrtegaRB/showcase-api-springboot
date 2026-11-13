package cl.valdora.api.config;

import cl.valdora.api.cliente.Cliente;
import cl.valdora.api.cliente.ClienteRepository;
import cl.valdora.api.comun.Rut;
import cl.valdora.api.producto.Categoria;
import cl.valdora.api.producto.Producto;
import cl.valdora.api.producto.ProductoRepository;
import cl.valdora.api.solicitud.EstadoSolicitud;
import cl.valdora.api.solicitud.Prioridad;
import cl.valdora.api.solicitud.SolicitudRepository;
import cl.valdora.api.solicitud.SolicitudServicio;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** Carga datos FICTICIOS al iniciar (desactivable con app.datos-demo=false). */
@Component
@ConditionalOnProperty(prefix = "app", name = "datos-demo", havingValue = "true")
public class DatosIniciales implements CommandLineRunner {

    private final ClienteRepository clientes;
    private final ProductoRepository productos;
    private final SolicitudRepository solicitudes;

    public DatosIniciales(ClienteRepository clientes, ProductoRepository productos, SolicitudRepository solicitudes) {
        this.clientes = clientes;
        this.productos = productos;
        this.solicitudes = solicitudes;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (clientes.count() > 0) {
            return;
        }
        String[][] datosClientes = {
                {"Bodegas Arrayán Sur SpA", "Santiago"}, {"Comercial Maitén Ltda.", "Providencia"},
                {"Distribuidora Lenga S.A.", "Las Condes"}, {"Logística Peumo Alto SpA", "Ñuñoa"},
                {"Alimentos Coigüe Ltda.", "Maipú"}, {"Clínica Quillay S.A.", "Pudahuel"},
                {"Colegio Canelo Norte SpA", "Quilicura"}, {"Hotel Tralca Ltda.", "San Bernardo"}};
        List<Cliente> listaClientes = new ArrayList<>();
        for (int i = 0; i < datosClientes.length; i++) {
            int cuerpo = 76_100_000 + i * 7_919;
            Cliente c = new Cliente();
            c.setRut(cuerpo + "-" + Rut.calcularDv(cuerpo));
            c.setRazonSocial(datosClientes[i][0]);
            c.setEmail("contacto@cliente" + (i + 1) + ".example");
            c.setTelefono(String.format("+56 9 5555 %04d", 100 + i));
            c.setComuna(datosClientes[i][1]);
            c.setActivo(i != 7);
            c.setFechaRegistro(LocalDate.now().minusDays(30L * (8 - i)));
            listaClientes.add(c);
        }
        clientes.saveAll(listaClientes);

        Object[][] datosProductos = {
                {"REP-1001", "Filtro de aire 24x24 pulgadas", Categoria.REPUESTOS, 12990, 48},
                {"REP-1004", "Bomba de condensado 220 V", Categoria.REPUESTOS, 64990, 4},
                {"HER-2001", "Multímetro digital True RMS", Categoria.HERRAMIENTAS, 89990, 9},
                {"HER-2002", "Termómetro infrarrojo -50 a 550 °C", Categoria.HERRAMIENTAS, 45990, 3},
                {"CLI-3001", "Refrigerante R-410A, cilindro 11 kg", Categoria.CLIMATIZACION, 129990, 8},
                {"CLI-3003", "Termostato programable", Categoria.CLIMATIZACION, 54990, 18},
                {"ELE-4001", "Disyuntor 2x20 A curva C", Categoria.ELECTRICIDAD, 14990, 60},
                {"ELE-4003", "Luminaria LED hermética 60 W", Categoria.ELECTRICIDAD, 38990, 22},
                {"SEG-5001", "Guantes dieléctricos clase 0", Categoria.SEGURIDAD, 34990, 10},
                {"SEG-5003", "Kit de bloqueo y etiquetado", Categoria.SEGURIDAD, 89990, 1}};
        List<Producto> listaProductos = new ArrayList<>();
        for (Object[] d : datosProductos) {
            Producto p = new Producto();
            p.setSku((String) d[0]);
            p.setNombre((String) d[1]);
            p.setCategoria((Categoria) d[2]);
            p.setPrecio(BigDecimal.valueOf((Integer) d[3]));
            p.setStock((Integer) d[4]);
            listaProductos.add(p);
        }
        productos.saveAll(listaProductos);

        String[] titulos = {"Mantención preventiva de chiller", "Revisión de tablero eléctrico", "Cambio de filtros de clima",
                "Termografía de motores", "Falla en bomba de condensado", "Instalación de luminarias LED"};
        EstadoSolicitud[] estados = EstadoSolicitud.values();
        Prioridad[] prioridades = Prioridad.values();
        List<SolicitudServicio> listaSolicitudes = new ArrayList<>();
        for (int i = 0; i < 12; i++) {
            LocalDateTime creada = LocalDateTime.now().minusDays(i * 3L).withNano(0);
            SolicitudServicio s = new SolicitudServicio();
            s.setCliente(listaClientes.get(i % 7));
            s.setProducto(i % 3 == 0 ? null : listaProductos.get(i % listaProductos.size()));
            s.setTitulo(titulos[i % titulos.length]);
            s.setDescripcion("Solicitud de demostración número " + (i + 1) + ".");
            s.setPrioridad(prioridades[i % prioridades.length]);
            s.setEstado(estados[i % estados.length]);
            s.setCreadaEn(creada);
            s.setActualizadaEn(creada.plusHours(2));
            listaSolicitudes.add(s);
        }
        solicitudes.saveAll(listaSolicitudes);
    }
}
