package com.umoar.minisuperhub.servicios;

import com.umoar.minisuperhub.modelos.DetalleFactura;
import com.umoar.minisuperhub.modelos.Factura;
import com.umoar.minisuperhub.modelos.Cliente;
import com.umoar.minisuperhub.modelos.Producto;
import com.umoar.minisuperhub.dto.VendedorFacturasResumen;
import com.umoar.minisuperhub.modelos.Usuario;
import com.umoar.minisuperhub.repositorios.ClienteRepository;
import com.umoar.minisuperhub.repositorios.FacturaRepository;
import com.umoar.minisuperhub.repositorios.ProductoRepository;
import com.umoar.minisuperhub.repositorios.UsuarioRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class FacturaService {

    private final FacturaRepository facturaRepository;
    private final ClienteRepository clienteRepository;
    private final ProductoRepository productoRepository;
    private final UsuarioRepository usuarioRepository;

    public List<Factura> listar() {
        return facturaRepository.findAllByOrderByIdDesc();
    }

    public List<Factura> listarPorVendedor(String username) {
        return facturaRepository.findByVendedorUsernameOrderByIdDesc(username);
    }

    @Transactional(readOnly = true)
    public List<VendedorFacturasResumen> resumirFacturasPorVendedor() {
        return facturaRepository.resumirFacturasPorVendedor();
    }

    @Transactional(readOnly = true)
    public List<Factura> listarParaReporte(LocalDateTime inicio, LocalDateTime fin) {
        return facturaRepository.buscarParaReporte(inicio, fin);
    }

    public Optional<Factura> buscarPorId(Long id) {
        return facturaRepository.findById(id);
    }

    public Optional<Factura> buscarPorIdYVendedor(Long id, String username) {
        return facturaRepository.findByIdAndVendedorUsername(id, username);
    }

    @Transactional(readOnly = true)
    public Optional<Factura> buscarCompletaPorId(Long id) {
        return facturaRepository.buscarCompletaPorId(id);
    }

    @Transactional(readOnly = true)
    public Optional<Factura> buscarCompletaPorIdYVendedor(Long id, String username) {
        return facturaRepository.buscarCompletaPorIdYVendedor(id, username);
    }

    @Transactional
    public Factura guardarDesdeFormulario(Factura formulario, Long clienteId,
                                         List<Long> productoIds, List<Integer> cantidades,
                                         String username, boolean administrador) {
        if (productoIds == null || cantidades == null || productoIds.isEmpty()
                || productoIds.size() != cantidades.size()) {
            throw new IllegalArgumentException("Agrega al menos un producto a la factura.");
        }

        if (formulario.getId() != 0 && !administrador
                && facturaRepository.findByIdAndVendedorUsername(formulario.getId(), username).isEmpty()) {
            throw new AccessDeniedException("Solo puedes modificar tus propias facturas.");
        }

        Cliente cliente = null;
        if (clienteId != null) {
            cliente = clienteRepository.findById(clienteId)
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Selecciona un cliente válido o consumidor final."));
        }

        List<DetalleFactura> detalles = new ArrayList<>();
        double total = 0;
        for (int i = 0; i < productoIds.size(); i++) {
            Long productoId = productoIds.get(i);
            Integer cantidad = cantidades.get(i);
            if (productoId == null || cantidad == null || cantidad < 1) {
                throw new IllegalArgumentException("Revisa los productos y cantidades de la factura.");
            }
            Producto producto = productoRepository.findById(productoId)
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Revisa los productos y cantidades de la factura."));
            double subtotal = producto.getPrecioVenta() * cantidad;
            detalles.add(DetalleFactura.builder()
                    .producto(producto)
                    .cantidad(cantidad)
                    .precioUnitario(producto.getPrecioVenta())
                    .subtotal(subtotal)
                    .build());
            total += subtotal;
        }

        Usuario vendedor = usuarioRepository.findByUsername(username)
                .orElseThrow(() -> new EntityNotFoundException("No se encontró el usuario autenticado."));
        Factura datosFactura = new Factura();
        datosFactura.setId(formulario.getId());
        datosFactura.setFecha(formulario.getFecha());
        datosFactura.setCliente(cliente);
        datosFactura.setTotal(total);
        return guardarConDetalles(datosFactura, detalles, vendedor);
    }

    @Transactional
    public Factura guardarConDetalles(Factura datosFactura, List<DetalleFactura> detalles, Usuario vendedor) {
        Factura factura = datosFactura;
        if (datosFactura.getId() != 0) {
            factura = facturaRepository.findById(datosFactura.getId())
                    .orElseThrow(() -> new EntityNotFoundException(
                            "No se encontró la factura con id " + datosFactura.getId()));
            if (factura.isAnulada()) {
                throw new IllegalStateException("No se puede modificar una factura anulada.");
            }
            factura.setFecha(datosFactura.getFecha());
            factura.setCliente(datosFactura.getCliente());
            factura.setTotal(datosFactura.getTotal());
        } else {
            factura.setVendedor(vendedor);
            factura.setAnulada(false);
        }

        if (factura.getDetalles() == null) {
            factura.setDetalles(new ArrayList<>());
        } else {
            factura.getDetalles().clear();
        }
        for (DetalleFactura detalle : detalles) {
            detalle.setFactura(factura);
            factura.getDetalles().add(detalle);
        }
        return facturaRepository.save(factura);
    }

    @Transactional
    public Factura agregarProducto(Long facturaId, Long productoId, int cantidad) {
        if (cantidad < 1) {
            throw new IllegalArgumentException("La cantidad debe ser mayor que cero.");
        }
        Producto producto = productoRepository.findById(productoId)
                .orElseThrow(() -> new EntityNotFoundException("No se encontró el producto con id " + productoId));
        double subtotal = producto.getPrecioVenta() * cantidad;
        DetalleFactura detalle = DetalleFactura.builder()
                .producto(producto)
                .cantidad(cantidad)
                .precioUnitario(producto.getPrecioVenta())
                .subtotal(subtotal)
                .build();
        return agregarDetalle(facturaId, detalle);
    }

    @Transactional
    public Factura agregarDetalle(Long facturaId, DetalleFactura detalle) {
        Factura factura = facturaRepository.buscarCompletaPorId(facturaId)
                .orElseThrow(() -> new EntityNotFoundException("No se encontró la factura con id " + facturaId));
        if (factura.isAnulada()) {
            throw new IllegalStateException("No se puede modificar una factura anulada.");
        }
        if (factura.getDetalles() == null) {
            factura.setDetalles(new ArrayList<>());
        }
        detalle.setFactura(factura);
        factura.getDetalles().add(detalle);
        factura.setTotal(factura.getTotal() + detalle.getSubtotal());
        return facturaRepository.save(factura);
    }

    @Transactional
    public Factura marcarEntregada(Long id) {
        Factura factura = facturaRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("No se encontró la factura con id " + id));
        if (factura.isAnulada()) {
            throw new IllegalStateException("No se puede entregar una factura anulada.");
        }
        factura.setEntregada(true);
        return facturaRepository.save(factura);
    }

    @Transactional
    public Factura anular(Long id) {
        Factura factura = facturaRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("No se encontró la factura con id " + id));
        factura.setAnulada(true);
        return facturaRepository.save(factura);
    }

    public void eliminar(Long id) {
        if (!facturaRepository.existsById(id)) {
            throw new EntityNotFoundException("No se encontró la factura con id " + id);
        }
        facturaRepository.deleteById(id);
    }
}
