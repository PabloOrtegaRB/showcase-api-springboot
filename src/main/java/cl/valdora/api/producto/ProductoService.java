package cl.valdora.api.producto;

import cl.valdora.api.comun.Excepciones.Conflicto;
import cl.valdora.api.comun.Excepciones.RecursoNoEncontrado;
import cl.valdora.api.comun.Pagina;
import cl.valdora.api.solicitud.SolicitudRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
@Transactional(readOnly = true)
public class ProductoService {

    private final ProductoRepository productos;
    private final SolicitudRepository solicitudes;

    public ProductoService(ProductoRepository productos, SolicitudRepository solicitudes) {
        this.productos = productos;
        this.solicitudes = solicitudes;
    }

    public Pagina<ProductoSalida> buscar(String texto, Categoria categoria, Boolean activo, int pagina, int tamano) {
        PageRequest solicitud = PageRequest.of(pagina, tamano, Sort.by("nombre").ascending());
        return Pagina.desde(productos.findAll(ProductoRepository.filtro(texto, categoria, activo), solicitud), ProductoSalida::desde);
    }

    public ProductoSalida obtener(Long id) {
        return ProductoSalida.desde(buscarEntidad(id));
    }

    @Transactional
    public ProductoSalida crear(ProductoEntrada entrada) {
        String sku = entrada.sku().trim().toUpperCase(Locale.ROOT);
        if (productos.existsBySku(sku)) {
            throw new Conflicto("Ya existe un producto con el SKU " + sku + ".");
        }
        Producto producto = new Producto();
        aplicar(producto, entrada, sku);
        return ProductoSalida.desde(productos.save(producto));
    }

    @Transactional
    public ProductoSalida actualizar(Long id, ProductoEntrada entrada) {
        Producto producto = buscarEntidad(id);
        String sku = entrada.sku().trim().toUpperCase(Locale.ROOT);
        if (productos.existsBySkuAndIdNot(sku, id)) {
            throw new Conflicto("Ya existe otro producto con el SKU " + sku + ".");
        }
        aplicar(producto, entrada, sku);
        return ProductoSalida.desde(productos.save(producto));
    }

    @Transactional
    public void eliminar(Long id) {
        Producto producto = buscarEntidad(id);
        if (solicitudes.existsByProductoId(id)) {
            throw new Conflicto("El producto está asociado a solicitudes: desactívalo en lugar de eliminarlo.");
        }
        productos.delete(producto);
    }

    private Producto buscarEntidad(Long id) {
        return productos.findById(id).orElseThrow(() -> new RecursoNoEncontrado("No existe el producto " + id + "."));
    }

    private static void aplicar(Producto producto, ProductoEntrada entrada, String sku) {
        producto.setSku(sku);
        producto.setNombre(entrada.nombre().trim());
        producto.setCategoria(entrada.categoria());
        producto.setPrecio(entrada.precio());
        producto.setStock(entrada.stock());
        producto.setActivo(entrada.activo() == null || entrada.activo());
    }
}
