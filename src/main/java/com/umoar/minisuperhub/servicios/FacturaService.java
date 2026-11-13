package com.umoar.minisuperhub.servicios;

import com.umoar.minisuperhub.modelos.DetalleFactura;
import com.umoar.minisuperhub.modelos.Factura;
import com.umoar.minisuperhub.modelos.Producto;
import com.umoar.minisuperhub.modelos.Usuario;
import com.umoar.minisuperhub.repositorios.FacturaRepository;
import com.umoar.minisuperhub.repositorios.ProductoRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
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
    private final ProductoRepository productoRepository;

    public List<Factura> listar() {
        return facturaRepository.findAllByOrderByIdDesc();
    }

    public List<Factura> listarPorVendedor(String username) {
        return facturaRepository.findByVendedorUsernameOrderByIdDesc(username);
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

    @Transactional(readOnly = true)
    public List<Factura> listarPorRango(LocalDateTime inicio, LocalDateTime fin) {
        return facturaRepository.buscarCompletasPorFechaBetween(inicio, fin);
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
            // Si la factura existente no estaba anulada, devolvemos temporalmente el stock previo para recalcular
            if (factura.getDetalles() != null) {
                for (DetalleFactura antiguoDetalle : factura.getDetalles()) {
                    if (antiguoDetalle.getProducto() != null) {
                        Producto prod = productoRepository.findById(antiguoDetalle.getProducto().getId())
                                .orElse(antiguoDetalle.getProducto());
                        prod.setStock(prod.getStock() + antiguoDetalle.getCantidad());
                        productoRepository.save(prod);
                    }
                }
            }
            factura.setFecha(datosFactura.getFecha());
            factura.setCliente(datosFactura.getCliente());
            factura.setTotal(datosFactura.getTotal());
        } else {
            factura.setVendedor(vendedor);
            factura.setAnulada(false);
        }

        // Validación y descuento de existencias para cada producto
        for (DetalleFactura detalle : detalles) {
            if (detalle.getProducto() != null) {
                Producto prod = productoRepository.findById(detalle.getProducto().getId())
                        .orElseThrow(() -> new EntityNotFoundException("No se encontró el producto con id " + detalle.getProducto().getId()));
                if (prod.getStock() < detalle.getCantidad()) {
                    throw new IllegalStateException("El producto '" + prod.getNombre() + "' solo cuenta con "
                            + prod.getStock() + " unidad(es) en existencia (solicitadas: " + detalle.getCantidad() + ").");
                }
                prod.setStock(prod.getStock() - detalle.getCantidad());
                productoRepository.save(prod);
                detalle.setProducto(prod);
            }
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
        if (factura.isAnulada()) {
            return factura;
        }

        // Reintegro automático de stock a cada producto
        if (factura.getDetalles() != null) {
            for (DetalleFactura detalle : factura.getDetalles()) {
                if (detalle.getProducto() != null) {
                    Producto prod = productoRepository.findById(detalle.getProducto().getId())
                            .orElse(detalle.getProducto());
                    prod.setStock(prod.getStock() + detalle.getCantidad());
                    productoRepository.save(prod);
                }
            }
        }

        factura.setAnulada(true);
        return facturaRepository.save(factura);
    }

    @Transactional
    public void eliminar(Long id) {
        Factura factura = facturaRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("No se encontró la factura con id " + id));

        // Si no estaba anulada, reintegrar stock antes de borrar definitivamente
        if (!factura.isAnulada() && factura.getDetalles() != null) {
            for (DetalleFactura detalle : factura.getDetalles()) {
                if (detalle.getProducto() != null) {
                    Producto prod = productoRepository.findById(detalle.getProducto().getId())
                            .orElse(detalle.getProducto());
                    prod.setStock(prod.getStock() + detalle.getCantidad());
                    productoRepository.save(prod);
                }
            }
        }
        facturaRepository.delete(factura);
    }
}
