package com.umoar.minisuperhub.repositorios;


import com.umoar.minisuperhub.modelos.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByUsername(String username);

    boolean existsByUsername(String username);

    boolean existsByUsernameAndIdNot(String username, Long id);

    @Query("select count(distinct usuario) from Usuario usuario join usuario.roles rol " +
            "where rol.nombre = :nombre and usuario.activo = true")
    long countActivosPorRol(@Param("nombre") String nombre);

    List<Usuario> findAllByOrderByNombreAsc();
}
