package com.umoar.minisuperhub.dto;

import java.time.LocalDate;
import java.util.List;

public record ReporteVentas(
        LocalDate fechaInicio,
        LocalDate fechaFin,
        boolean rangoValido,
        double totalVentas,
        long totalFacturas,
        double ingresos,
        Double variacionVentas,
        Double variacionFacturas,
        Double variacionIngresos,
        List<ProductoMasVendido> productosMasVendidos,
        int cantidadCategorias,
        int cantidadClientes,
        int cantidadProductos,
        int cantidadFacturas,
        int cantidadAnuladas) {
}
