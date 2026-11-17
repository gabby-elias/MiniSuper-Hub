package com.umoar.minisuperhub.servicios;

import com.umoar.minisuperhub.dto.ProductoMasVendido;
import com.umoar.minisuperhub.dto.ReporteVentas;
import com.umoar.minisuperhub.modelos.DetalleFactura;
import com.umoar.minisuperhub.modelos.Factura;
import com.umoar.minisuperhub.modelos.Producto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ReporteVentasService {

    private static final ZoneId ZONA_HORARIA = ZoneId.of("America/El_Salvador");

    private final CategoriaService categoriaService;
    private final ClienteService clienteService;
    private final FacturaService facturaService;
    private final ProductoService productoService;

    @Transactional(readOnly = true)
    public ReporteVentas generar(LocalDate fechaInicio, LocalDate fechaFin) {
        LocalDate hoy = LocalDate.now(ZONA_HORARIA);
        LocalDate inicio = fechaInicio != null ? fechaInicio : hoy.withDayOfMonth(1);
        LocalDate fin = fechaFin != null ? fechaFin : hoy;
        boolean rangoValido = !inicio.isAfter(fin);

        List<Factura> facturasPeriodo = rangoValido
                ? facturaService.listarParaReporte(inicio.atStartOfDay(), fin.plusDays(1).atStartOfDay())
                : List.of();
        LocalDate inicioAnterior = rangoValido ? inicioPeriodoAnterior(inicio, fin) : inicio;
        List<Factura> facturasPeriodoAnterior = rangoValido
                ? facturaService.listarParaReporte(inicioAnterior.atStartOfDay(), inicio.atStartOfDay())
                : List.of();

        List<Factura> facturasVigentes = facturasPeriodo.stream()
                .filter(factura -> !factura.isAnulada())
                .toList();
        List<Factura> facturasVigentesAnteriores = facturasPeriodoAnterior.stream()
                .filter(factura -> !factura.isAnulada())
                .toList();
        double totalVentas = sumarTotales(facturasVigentes);

        return new ReporteVentas(
                inicio,
                fin,
                rangoValido,
                totalVentas,
                facturasPeriodo.size(),
                totalVentas,
                rangoValido ? porcentajeCambio(totalVentas, sumarTotales(facturasVigentesAnteriores)) : null,
                rangoValido ? porcentajeCambio(facturasPeriodo.size(), facturasPeriodoAnterior.size()) : null,
                rangoValido ? porcentajeCambio(totalVentas, sumarTotales(facturasVigentesAnteriores)) : null,
                productosMasVendidos(facturasVigentes),
                categoriaService.listar().size(),
                clienteService.listar().size(),
                productoService.listar().size(),
                facturasVigentes.size(),
                facturasPeriodo.size() - facturasVigentes.size());
    }

    private LocalDate inicioPeriodoAnterior(LocalDate inicio, LocalDate fin) {
        long diasPeriodo = ChronoUnit.DAYS.between(inicio, fin) + 1;
        return inicio.minusDays(diasPeriodo);
    }

    private double sumarTotales(List<Factura> facturas) {
        return facturas.stream().mapToDouble(Factura::getTotal).sum();
    }

    private Double porcentajeCambio(double actual, double anterior) {
        if (anterior == 0) {
            return actual == 0 ? 0.0 : null;
        }
        return ((actual - anterior) / anterior) * 100.0;
    }

    private List<ProductoMasVendido> productosMasVendidos(List<Factura> facturas) {
        Map<Long, ProductSales> ventasPorProducto = new HashMap<>();
        for (Factura factura : facturas) {
            if (factura.getDetalles() == null) {
                continue;
            }
            for (DetalleFactura detalle : factura.getDetalles()) {
                Producto producto = detalle.getProducto();
                if (producto == null) {
                    continue;
                }
                ventasPorProducto.merge(producto.getId(),
                        new ProductSales(producto.getNombre(), detalle.getCantidad()),
                        (actual, nueva) -> new ProductSales(actual.nombre(), actual.unidades() + nueva.unidades()));
            }
        }

        int maxUnidades = ventasPorProducto.values().stream()
                .mapToInt(ProductSales::unidades)
                .max()
                .orElse(0);

        return ventasPorProducto.values().stream()
                .sorted(Comparator.comparingInt(ProductSales::unidades).reversed()
                        .thenComparing(ProductSales::nombre))
                .limit(5)
                .map(producto -> new ProductoMasVendido(producto.nombre(), producto.unidades(),
                        maxUnidades == 0 ? 0 : (int) Math.round(producto.unidades() * 100.0 / maxUnidades)))
                .toList();
    }

    private record ProductSales(String nombre, int unidades) {
    }
}
