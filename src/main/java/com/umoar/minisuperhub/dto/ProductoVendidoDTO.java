package com.umoar.minisuperhub.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductoVendidoDTO {
    private String nombre;
    private int cantidad;
    private double totalRecaudado;
}
