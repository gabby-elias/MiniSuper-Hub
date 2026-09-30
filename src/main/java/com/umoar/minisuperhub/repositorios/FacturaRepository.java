package com.umoar.minisuperhub.repositorios;

import com.umoar.minisuperhub.modelos.Factura;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FacturaRepository extends JpaRepository<Factura, Long> {
}
