package com.umoar.minisuperhub.repositorios;

import com.umoar.minisuperhub.modelos.Producto;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductoRepository extends JpaRepository<Producto, Long> {
}
