package com.umoar.minisuperhub.servicios;

import com.umoar.minisuperhub.modelos.DetalleFactura;
import com.umoar.minisuperhub.modelos.Factura;
import com.umoar.minisuperhub.modelos.Usuario;
import com.umoar.minisuperhub.repositorios.FacturaRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class FacturaService {

    private final FacturaRepository facturaRepository;

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
