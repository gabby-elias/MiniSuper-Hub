package com.umoar.minisuperhub.repositorios;

import com.umoar.minisuperhub.modelos.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClienteRepository extends JpaRepository<Cliente, Long> {
}
