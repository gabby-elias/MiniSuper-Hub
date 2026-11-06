package com.umoar.minisuperhub.controladores;

import com.umoar.minisuperhub.dto.ProductoVendidoDTO;
import com.umoar.minisuperhub.dto.VendedorResumenDTO;
import com.umoar.minisuperhub.modelos.DetalleFactura;
import com.umoar.minisuperhub.modelos.Factura;
import com.umoar.minisuperhub.servicios.CategoriaService;
import com.umoar.minisuperhub.servicios.ClienteService;
import com.umoar.minisuperhub.servicios.FacturaExcelService;
import com.umoar.minisuperhub.servicios.FacturaService;
import com.umoar.minisuperhub.servicios.ProductoService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequiredArgsConstructor
public class ReportesController {

    private final CategoriaService categoriaService;
    private final ClienteService clienteService;
    private final FacturaService facturaService;
    private final ProductoService productoService;
    private final FacturaExcelService facturaExcelService;

    private static final ZoneId ZONA_HORARIA = ZoneId.of("America/El_Salvador");

    @GetMapping("/reportes")
    public String mostrar(
            @RequestParam(required = false, defaultValue = "hoy") String periodo,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin,
            Model model) {

        LocalDate hoy = LocalDate.now(ZONA_HORARIA);
        LocalDate[] fechas = resolverRangoFechas(periodo, fechaInicio, fechaFin, hoy);
        LocalDate inicio = fechas[0];
        LocalDate fin = fechas[1];

        LocalDateTime inicioLdt = inicio.atStartOfDay();
        LocalDateTime finLdt = fin.atTime(LocalTime.MAX);

        // Listar facturas filtradas por rango
        List<Factura> facturasPeriodo = facturaService.listarPorRango(inicioLdt, finLdt);

        List<Factura> vigentesPeriodo = facturasPeriodo.stream()
                .filter(factura -> !factura.isAnulada())
                .toList();

        double totalVentasPeriodo = vigentesPeriodo.stream()
                .mapToDouble(Factura::getTotal)
                .sum();

        long cantidadFacturasPeriodo = vigentesPeriodo.size();
        long cantidadAnuladasPeriodo = facturasPeriodo.size() - vigentesPeriodo.size();
        double ticketPromedio = cantidadFacturasPeriodo > 0
                ? totalVentasPeriodo / cantidadFacturasPeriodo
                : 0.0;

        // Top productos más vendidos
        List<ProductoVendidoDTO> topProductos = calcularTopProductos(vigentesPeriodo);

        // Ventas por cajero / vendedor
        List<VendedorResumenDTO> ventasPorVendedor = calcularVentasPorVendedor(vigentesPeriodo);

        // Variables históricas / globales para conservar coherencia y tests
        var todasFacturas = facturaService.listar();
        var todasVigentes = todasFacturas.stream().filter(f -> !f.isAnulada()).toList();

        model.addAttribute("cantidadCategorias", categoriaService.listar().size());
        model.addAttribute("cantidadClientes", clienteService.listar().size());
        model.addAttribute("cantidadProductos", productoService.listar().size());
        model.addAttribute("cantidadFacturas", todasVigentes.size());
        model.addAttribute("cantidadAnuladas", todasFacturas.size() - todasVigentes.size());
        model.addAttribute("totalVentas", todasVigentes.stream().mapToDouble(Factura::getTotal).sum());

        // Datos del período seleccionado
        model.addAttribute("periodo", periodo);
        model.addAttribute("fechaInicio", inicio);
        model.addAttribute("fechaFin", fin);
        model.addAttribute("facturasPeriodo", facturasPeriodo);
        model.addAttribute("totalVentasPeriodo", totalVentasPeriodo);
        model.addAttribute("ticketPromedio", ticketPromedio);
        model.addAttribute("cantidadFacturasPeriodo", cantidadFacturasPeriodo);
        model.addAttribute("cantidadAnuladasPeriodo", cantidadAnuladasPeriodo);
        model.addAttribute("topProductos", topProductos);
        model.addAttribute("ventasPorVendedor", ventasPorVendedor);

        return "reportes/index";
    }

    @GetMapping("/reportes/excel")
    public ResponseEntity<byte[]> exportarExcel(
            @RequestParam(required = false, defaultValue = "hoy") String periodo,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin) {

        LocalDate hoy = LocalDate.now(ZONA_HORARIA);
        LocalDate[] fechas = resolverRangoFechas(periodo, fechaInicio, fechaFin, hoy);
        LocalDate inicio = fechas[0];
        LocalDate fin = fechas[1];

        LocalDateTime inicioLdt = inicio.atStartOfDay();
        LocalDateTime finLdt = fin.atTime(LocalTime.MAX);

        List<Factura> facturasPeriodo = facturaService.listarPorRango(inicioLdt, finLdt);
        List<Factura> vigentesPeriodo = facturasPeriodo.stream().filter(f -> !f.isAnulada()).toList();

        double totalVentas = vigentesPeriodo.stream().mapToDouble(Factura::getTotal).sum();
        long cantVigentes = vigentesPeriodo.size();
        long cantAnuladas = facturasPeriodo.size() - cantVigentes;
        double ticketPromedio = cantVigentes > 0 ? totalVentas / cantVigentes : 0.0;

        List<ProductoVendidoDTO> topProductos = calcularTopProductos(vigentesPeriodo);
        List<VendedorResumenDTO> ventasPorVendedor = calcularVentasPorVendedor(vigentesPeriodo);

        byte[] contenido = facturaExcelService.generarReporteVentasPeriodo(
                inicio, fin, facturasPeriodo, totalVentas, ticketPromedio,
                cantVigentes, cantAnuladas, topProductos, ventasPorVendedor);

        String nombreArchivo = String.format("Reporte_Ventas_%s_al_%s.xlsx", inicio, fin);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + nombreArchivo + "\"")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(contenido);
    }

    private LocalDate[] resolverRangoFechas(String periodo, LocalDate inicioParam, LocalDate finParam, LocalDate hoy) {
        if ("personalizado".equalsIgnoreCase(periodo) && inicioParam != null && finParam != null) {
            return new LocalDate[]{inicioParam, finParam};
        }
        if ("7dias".equalsIgnoreCase(periodo)) {
            return new LocalDate[]{hoy.minusDays(6), hoy};
        }
        if ("mes".equalsIgnoreCase(periodo)) {
            return new LocalDate[]{hoy.withDayOfMonth(1), hoy};
        }
        // "hoy" por defecto
        return new LocalDate[]{hoy, hoy};
    }

    private List<ProductoVendidoDTO> calcularTopProductos(List<Factura> facturasVigentes) {
        Map<String, int[]> acumulado = new HashMap<>(); // [0] = cantidad, [1] = subtotal en centavos
        for (Factura f : facturasVigentes) {
            if (f.getDetalles() == null) continue;
            for (DetalleFactura d : f.getDetalles()) {
                String nombreProd = d.getProducto() != null ? d.getProducto().getNombre() : "Producto sin nombre";
                acumulado.putIfAbsent(nombreProd, new int[]{0, 0});
                acumulado.get(nombreProd)[0] += d.getCantidad();
                acumulado.get(nombreProd)[1] += (int) Math.round(d.getSubtotal() * 100);
            }
        }

        List<ProductoVendidoDTO> lista = new ArrayList<>();
        for (Map.Entry<String, int[]> e : acumulado.entrySet()) {
            lista.add(ProductoVendidoDTO.builder()
                    .nombre(e.getKey())
                    .cantidad(e.getValue()[0])
                    .totalRecaudado(e.getValue()[1] / 100.0)
                    .build());
        }

        lista.sort(Comparator.comparingInt(ProductoVendidoDTO::getCantidad).reversed());
        if (lista.size() > 5) {
            return lista.subList(0, 5);
        }
        return lista;
    }

    private List<VendedorResumenDTO> calcularVentasPorVendedor(List<Factura> facturasVigentes) {
        Map<String, Object[]> mapa = new HashMap<>(); // [0] = long cantidad, [1] = double total
        for (Factura f : facturasVigentes) {
            String nombre = f.getVendedor() != null ? f.getVendedor().getNombre() : "Sin asignar";
            mapa.putIfAbsent(nombre, new Object[]{0L, 0.0});
            Object[] datos = mapa.get(nombre);
            datos[0] = ((Long) datos[0]) + 1;
            datos[1] = ((Double) datos[1]) + f.getTotal();
        }

        List<VendedorResumenDTO> lista = new ArrayList<>();
        for (Map.Entry<String, Object[]> e : mapa.entrySet()) {
            lista.add(VendedorResumenDTO.builder()
                    .nombre(e.getKey())
                    .cantidadFacturas((Long) e.getValue()[0])
                    .totalVendido((Double) e.getValue()[1])
                    .build());
        }

        lista.sort(Comparator.comparingDouble(VendedorResumenDTO::getTotalVendido).reversed());
        return lista;
    }
}
