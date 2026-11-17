package com.umoar.minisuperhub.servicios;

import com.umoar.minisuperhub.modelos.DetalleFactura;
import com.umoar.minisuperhub.modelos.Factura;
import com.umoar.minisuperhub.modelos.Producto;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ReporteVentasServiceTests {

    private final CategoriaService categoriaService = mock(CategoriaService.class);
    private final ClienteService clienteService = mock(ClienteService.class);
    private final FacturaService facturaService = mock(FacturaService.class);
    private final ProductoService productoService = mock(ProductoService.class);
    private final ReporteVentasService reporteService = new ReporteVentasService(
            categoriaService, clienteService, facturaService, productoService);

    @Test
    void generaTotalesYRankingParaElRangoSolicitado() {
        LocalDate inicio = LocalDate.of(2025, 3, 1);
        LocalDate fin = LocalDate.of(2025, 3, 31);
        Producto producto = Producto.builder().id(3L).nombre("Arroz").build();
        Factura facturaActual = Factura.builder()
                .id(1L)
                .total(20.0)
                .detalles(List.of(DetalleFactura.builder().producto(producto).cantidad(4).build()))
                .build();
        Factura facturaAnterior = Factura.builder().id(2L).total(10.0).build();

        when(facturaService.listarParaReporte(LocalDateTime.of(2025, 3, 1, 0, 0),
                LocalDateTime.of(2025, 4, 1, 0, 0))).thenReturn(List.of(facturaActual));
        when(facturaService.listarParaReporte(LocalDateTime.of(2025, 1, 29, 0, 0),
                LocalDateTime.of(2025, 3, 1, 0, 0))).thenReturn(List.of(facturaAnterior));
        when(categoriaService.listar()).thenReturn(List.of());
        when(clienteService.listar()).thenReturn(List.of());
        when(productoService.listar()).thenReturn(List.of());

        var reporte = reporteService.generar(inicio, fin);

        assertEquals(20.0, reporte.totalVentas(), 0.0001);
        assertEquals(1, reporte.totalFacturas());
        assertEquals(100.0, reporte.variacionVentas(), 0.0001);
        assertEquals(1, reporte.productosMasVendidos().size());
        assertEquals("Arroz", reporte.productosMasVendidos().get(0).nombre());
        assertEquals(4, reporte.productosMasVendidos().get(0).unidades());
        assertEquals(100, reporte.productosMasVendidos().get(0).porcentaje());
    }
}
