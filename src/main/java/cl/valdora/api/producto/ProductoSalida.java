package cl.valdora.api.producto;

import java.math.BigDecimal;

public record ProductoSalida(Long id, String sku, String nombre, Categoria categoria, BigDecimal precio,
                             int stock, boolean activo) {

    public static ProductoSalida desde(Producto p) {
        return new ProductoSalida(p.getId(), p.getSku(), p.getNombre(), p.getCategoria(), p.getPrecio(),
                p.getStock(), p.isActivo());
    }
}
