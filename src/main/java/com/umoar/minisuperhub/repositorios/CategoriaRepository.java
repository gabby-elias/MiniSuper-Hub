package com.umoar.minisuperhub.repositorios;

import com.umoar.minisuperhub.modelos.Categoria;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoriaRepository extends JpaRepository<Categoria, Long> {
}
