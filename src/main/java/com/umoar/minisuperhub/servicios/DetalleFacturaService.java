package com.umoar.minisuperhub.servicios;

import com.umoar.minisuperhub.modelos.DetalleFactura;
import com.umoar.minisuperhub.repositorios.DetalleFacturaRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class DetalleFacturaService {

    private final DetalleFacturaRepository detalleFacturaRepository;

    public List<DetalleFactura> listar() {
        return detalleFacturaRepository.findAll();
    }

    public Optional<DetalleFactura> buscarPorId(Long id) {
        return detalleFacturaRepository.findById(id);
    }

    public DetalleFactura guardar(DetalleFactura detalleFactura) {
        return detalleFacturaRepository.save(detalleFactura);
    }

    public DetalleFactura actualizar(Long id, DetalleFactura detalleFactura) {
        if (!detalleFacturaRepository.existsById(id)) {
            throw new EntityNotFoundException("No se encontró el detalle de factura con id " + id);
        }
        detalleFactura.setId(id);
        return detalleFacturaRepository.save(detalleFactura);
    }

    public void eliminar(Long id) {
        if (!detalleFacturaRepository.existsById(id)) {
            throw new EntityNotFoundException("No se encontró el detalle de factura con id " + id);
        }
        detalleFacturaRepository.deleteById(id);
    }
}
