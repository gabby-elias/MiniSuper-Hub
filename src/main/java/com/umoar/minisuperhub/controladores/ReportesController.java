package com.umoar.minisuperhub.controladores;

import com.umoar.minisuperhub.modelos.DetalleFactura;
import com.umoar.minisuperhub.modelos.Factura;
import com.umoar.minisuperhub.modelos.Producto;
import com.umoar.minisuperhub.servicios.CategoriaService;
import com.umoar.minisuperhub.servicios.ClienteService;
import com.umoar.minisuperhub.servicios.FacturaService;
import com.umoar.minisuperhub.servicios.ProductoService;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;

import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;

import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequiredArgsConstructor
public class ReportesController {

    private static final ZoneId ZONA_HORARIA = ZoneId.of("America/El_Salvador");

    private final CategoriaService categoriaService;
    private final ClienteService clienteService;
    private final FacturaService facturaService;
    private final ProductoService productoService;

    @GetMapping("/reportes")
    @Transactional(readOnly = true)
    public String mostrar(
            @RequestParam(name = "fechaInicio", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
            @RequestParam(name = "fechaFin", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin,
            Model model) {
        LocalDate hoy = LocalDate.now(ZONA_HORARIA);
        LocalDate inicio = fechaInicio != null ? fechaInicio : hoy.withDayOfMonth(1);
        LocalDate fin = fechaFin != null ? fechaFin : hoy;
        boolean rangoValido = !inicio.isAfter(fin);

        List<Factura> todasLasFacturas = facturaService.listar();
        List<Factura> facturasPeriodo = rangoValido
                ? filtrarPorPeriodo(todasLasFacturas, inicio, fin)
                : List.of();
        List<Factura> facturasPeriodoAnterior = rangoValido
                ? filtrarPorPeriodo(todasLasFacturas, inicioPeriodoAnterior(inicio, fin), inicio.minusDays(1))
                : List.of();

        List<Factura> facturasVigentes = facturasPeriodo.stream()
                .filter(factura -> !factura.isAnulada())
                .toList();
        List<Factura> facturasVigentesAnteriores = facturasPeriodoAnterior.stream()
                .filter(factura -> !factura.isAnulada())
                .toList();

        double totalVentas = sumarTotales(facturasVigentes);
        double ingresos = totalVentas;

        model.addAttribute("fechaInicio", inicio);
        model.addAttribute("fechaFin", fin);
        model.addAttribute("rangoValido", rangoValido);
        model.addAttribute("totalVentas", totalVentas);
        model.addAttribute("totalFacturas", facturasPeriodo.size());
        model.addAttribute("ingresos", ingresos);
        model.addAttribute("variacionVentas", rangoValido
                ? porcentajeCambio(totalVentas, sumarTotales(facturasVigentesAnteriores)) : null);
        model.addAttribute("variacionFacturas", rangoValido
                ? porcentajeCambio(facturasPeriodo.size(), facturasPeriodoAnterior.size()) : null);
        model.addAttribute("variacionIngresos", rangoValido
                ? porcentajeCambio(ingresos, sumarTotales(facturasVigentesAnteriores)) : null);
        model.addAttribute("productosMasVendidos", productosMasVendidos(facturasVigentes));

        model.addAttribute("cantidadCategorias", categoriaService.listar().size());
        model.addAttribute("cantidadClientes", clienteService.listar().size());
        model.addAttribute("cantidadProductos", productoService.listar().size());
        model.addAttribute("cantidadFacturas", facturasVigentes.size());
        model.addAttribute("cantidadAnuladas", facturasPeriodo.size() - facturasVigentes.size());
        return "reportes/index";
    }

    @GetMapping("/reportes/pdf")
    @Transactional(readOnly = true)
    public ResponseEntity<byte[]> descargarPdf(
            @RequestParam(name = "fechaInicio", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaInicio,
            @RequestParam(name = "fechaFin", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaFin) {
        LocalDate hoy = LocalDate.now(ZONA_HORARIA);
        LocalDate inicio = fechaInicio != null ? fechaInicio : hoy.withDayOfMonth(1);
        LocalDate fin = fechaFin != null ? fechaFin : hoy;
        if (inicio.isAfter(fin)) {
            return ResponseEntity.badRequest().build();
        }

        List<Factura> facturasPeriodo = filtrarPorPeriodo(facturaService.listar(), inicio, fin);
        List<Factura> facturasVigentes = facturasPeriodo.stream()
                .filter(factura -> !factura.isAnulada())
                .toList();
        List<Map<String, Object>> productos = productosMasVendidos(facturasVigentes);

        byte[] pdf;
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4);
            PdfWriter.getInstance(document, output);
            document.open();
            Font titulo = new Font(Font.HELVETICA, 18, Font.BOLD);
            Font subtitulo = new Font(Font.HELVETICA, 11, Font.NORMAL);
            document.add(new Paragraph("Reporte de ventas - MiniSuper Hub", titulo));
            document.add(new Paragraph("Periodo: " + inicio.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                    + " al " + fin.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")), subtitulo));
            document.add(new Paragraph(" "));

            PdfPTable resumen = new PdfPTable(2);
            resumen.setWidthPercentage(100);
            resumen.addCell("Indicador");
            resumen.addCell("Resultado");
            resumen.addCell("Total de ventas");
            resumen.addCell(String.format(java.util.Locale.US, "$%.2f", sumarTotales(facturasVigentes)));
            resumen.addCell("Facturas emitidas");
            resumen.addCell(String.valueOf(facturasPeriodo.size()));
            resumen.addCell("Ingresos registrados");
            resumen.addCell(String.format(java.util.Locale.US, "$%.2f", sumarTotales(facturasVigentes)));
            document.add(resumen);
            document.add(new Paragraph(" "));
            document.add(new Paragraph("Productos más vendidos", new Font(Font.HELVETICA, 13, Font.BOLD)));

            PdfPTable ranking = new PdfPTable(new float[]{1, 5, 2});
            ranking.setWidthPercentage(100);
            ranking.addCell("#");
            ranking.addCell("Producto");
            ranking.addCell("Unidades");
            int posicion = 1;
            for (Map<String, Object> producto : productos) {
                ranking.addCell(String.valueOf(posicion++));
                ranking.addCell(String.valueOf(producto.get("nombre")));
                ranking.addCell(String.valueOf(producto.get("unidades")));
            }
            if (productos.isEmpty()) {
                ranking.addCell("—");
                ranking.addCell("Sin ventas de productos en este periodo");
                ranking.addCell("0");
            }
            document.add(ranking);
            document.add(new Paragraph(" "));
            document.add(new Paragraph("Los ingresos se calculan a partir de facturas no anuladas; el sistema no registra cobros por separado.", subtitulo));
            document.close();
            pdf = output.toByteArray();
        } catch (DocumentException exception) {
            throw new IllegalStateException("No se pudo generar el reporte PDF", exception);
        } catch (java.io.IOException exception) {
            throw new IllegalStateException("No se pudo finalizar el reporte PDF", exception);
        }

        String nombreArchivo = "reporte-" + inicio + "-" + fin + ".pdf";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + nombreArchivo + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    private List<Factura> filtrarPorPeriodo(List<Factura> facturas, LocalDate inicio, LocalDate fin) {
        return facturas.stream()
                .filter(factura -> factura.getFecha() != null)
                .filter(factura -> {
                    LocalDate fecha = factura.getFecha().toLocalDate();
                    return !fecha.isBefore(inicio) && !fecha.isAfter(fin);
                })
                .toList();
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

    private List<Map<String, Object>> productosMasVendidos(List<Factura> facturas) {
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
                .map(producto -> {
                    Map<String, Object> fila = new HashMap<>();
                    fila.put("nombre", producto.nombre());
                    fila.put("unidades", producto.unidades());
                    fila.put("porcentaje", maxUnidades == 0 ? 0
                            : (int) Math.round(producto.unidades() * 100.0 / maxUnidades));
                    return fila;
                })
                .toList();
    }

    private record ProductSales(String nombre, int unidades) {
    }
}
