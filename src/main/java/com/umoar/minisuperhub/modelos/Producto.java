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
    private double precioVenta;


    @ManyToOne
    @JoinColumn(name = "categoria_id")
    private Categoria categoria;
}
