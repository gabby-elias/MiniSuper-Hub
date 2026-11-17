package com.umoar.minisuperhub.dto;

public record ProductoMasVendido(String nombre, int unidades, int porcentaje) {

    public String getNombre() {
        return nombre;
    }

    public int getUnidades() {
        return unidades;
    }

    public int getPorcentaje() {
        return porcentaje;
    }
}
