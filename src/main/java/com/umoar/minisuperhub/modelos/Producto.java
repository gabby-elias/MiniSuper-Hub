package com.umoar.minisuperhub.modelos;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "productos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Producto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    private String nombre;
    private String unidadMedida;
    private int stock;

    private double precioVenta;

    // La tabla existente conserva ambas columnas como obligatorias.
    @Column(name = "precio")
    private double precio;

    @ManyToOne
    @JoinColumn(name = "categoria_id")
    private Categoria categoria;

    @PrePersist
    @PreUpdate
    private void sincronizarPrecioLegado() {
        precio = precioVenta;
    }
}
