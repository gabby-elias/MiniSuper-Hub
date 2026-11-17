package com.umoar.minisuperhub.repositorios;

import com.umoar.minisuperhub.dto.VendedorFacturasResumen;
import com.umoar.minisuperhub.modelos.Factura;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface FacturaRepository extends JpaRepository<Factura, Long> {

    List<Factura> findAllByOrderByIdDesc();

    List<Factura> findByVendedorUsernameOrderByIdDesc(String username);

    @Query("select new com.umoar.minisuperhub.dto.VendedorFacturasResumen(" +
            "u.username, u.nombre, count(f.id)) " +
            "from Usuario u join u.roles r " +
            "left join Factura f on f.vendedor = u " +
            "where r.nombre = 'ROLE_EMPLEADO' " +
            "group by u.username, u.nombre " +
            "order by u.nombre")
    List<VendedorFacturasResumen> resumirFacturasPorVendedor();

    @Query("select distinct f from Factura f " +
            "left join fetch f.detalles d " +
            "left join fetch d.producto " +
            "where f.fecha >= :inicio and f.fecha < :fin")
    List<Factura> buscarParaReporte(@Param("inicio") LocalDateTime inicio,
                                   @Param("fin") LocalDateTime fin);

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
