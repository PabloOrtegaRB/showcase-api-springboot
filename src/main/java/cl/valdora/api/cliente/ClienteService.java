package cl.valdora.api.cliente;

import cl.valdora.api.comun.Excepciones.Conflicto;
import cl.valdora.api.comun.Excepciones.RecursoNoEncontrado;
import cl.valdora.api.comun.Pagina;
import cl.valdora.api.comun.Rut;
import cl.valdora.api.solicitud.SolicitudRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Locale;

@Service
@Transactional(readOnly = true)
public class ClienteService {

    private final ClienteRepository clientes;
    private final SolicitudRepository solicitudes;
    private final Clock reloj;

    public ClienteService(ClienteRepository clientes, SolicitudRepository solicitudes, Clock reloj) {
        this.clientes = clientes;
        this.solicitudes = solicitudes;
        this.reloj = reloj;
    }

    public Pagina<ClienteSalida> buscar(String texto, String comuna, Boolean activo, int pagina, int tamano) {
        PageRequest solicitud = PageRequest.of(pagina, tamano, Sort.by("razonSocial").ascending());
        return Pagina.desde(clientes.findAll(ClienteRepository.filtro(texto, comuna, activo), solicitud), ClienteSalida::desde);
    }

    public ClienteSalida obtener(Long id) {
        return ClienteSalida.desde(buscarEntidad(id));
    }

    @Transactional
    public ClienteSalida crear(ClienteEntrada entrada) {
        String rut = Rut.normalizar(entrada.rut());
        if (clientes.existsByRut(rut)) {
            throw new Conflicto("Ya existe un cliente con el RUT " + rut + ".");
        }
        Cliente cliente = new Cliente();
        aplicar(cliente, entrada, rut);
        cliente.setFechaRegistro(LocalDate.now(reloj));
        return ClienteSalida.desde(clientes.save(cliente));
    }

    @Transactional
    public ClienteSalida actualizar(Long id, ClienteEntrada entrada) {
        Cliente cliente = buscarEntidad(id);
        String rut = Rut.normalizar(entrada.rut());
        if (clientes.existsByRutAndIdNot(rut, id)) {
            throw new Conflicto("Ya existe otro cliente con el RUT " + rut + ".");
        }
        aplicar(cliente, entrada, rut);
        return ClienteSalida.desde(clientes.save(cliente));
    }

    /** Un cliente con solicitudes no se elimina: se desactiva, para conservar el historial. */
    @Transactional
    public void eliminar(Long id) {
        Cliente cliente = buscarEntidad(id);
        if (solicitudes.existsByClienteId(id)) {
            throw new Conflicto("El cliente tiene solicitudes asociadas: desactívalo en lugar de eliminarlo.");
        }
        clientes.delete(cliente);
    }

    private Cliente buscarEntidad(Long id) {
        return clientes.findById(id).orElseThrow(() -> new RecursoNoEncontrado("No existe el cliente " + id + "."));
    }

    private static void aplicar(Cliente cliente, ClienteEntrada entrada, String rut) {
        cliente.setRut(rut);
        cliente.setRazonSocial(entrada.razonSocial().trim());
        cliente.setEmail(entrada.email().trim().toLowerCase(Locale.ROOT));
        String telefono = entrada.telefono();
        cliente.setTelefono(telefono == null || telefono.isBlank() ? null : telefono.trim());
        cliente.setComuna(entrada.comuna().trim());
        cliente.setActivo(entrada.activo() == null || entrada.activo());
    }
}
