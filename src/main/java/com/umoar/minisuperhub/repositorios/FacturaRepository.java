package com.umoar.minisuperhub.repositorios;

import com.umoar.minisuperhub.modelos.Factura;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface FacturaRepository extends JpaRepository<Factura, Long> {

    List<Factura> findAllByOrderByIdDesc();

    List<Factura> findByVendedorUsernameOrderByIdDesc(String username);

    Optional<Factura> findByIdAndVendedorUsername(Long id, String username);

    @Query("select distinct f from Factura f " +
            "left join fetch f.cliente " +
            "left join fetch f.vendedor " +
            "left join fetch f.detalles d " +
            "left join fetch d.producto " +
            "where f.id = :id")
    Optional<Factura> buscarCompletaPorId(@Param("id") Long id);

    @Query("select distinct f from Factura f " +
            "left join fetch f.cliente " +
            "left join fetch f.vendedor " +
            "left join fetch f.detalles d " +
            "left join fetch d.producto " +
            "where f.id = :id and f.vendedor.username = :username")
    Optional<Factura> buscarCompletaPorIdYVendedor(@Param("id") Long id,
                                                    @Param("username") String username);
}
