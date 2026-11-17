package com.umoar.minisuperhub.dto;

public record VendedorFacturasResumen(String username, String nombre, Long cantidadFacturas) {

    public String getUsername() {
        return username;
    }

    public String getNombre() {
        return nombre;
    }

    public Long getCantidadFacturas() {
        return cantidadFacturas;
    }
}
