package com.umoar.minisuperhub.controladores;

import com.umoar.minisuperhub.dto.ReporteVentas;
import com.umoar.minisuperhub.servicios.ReportePdfService;
import com.umoar.minisuperhub.servicios.ReporteVentasService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequiredArgsConstructor
public class ReportesController {

    private final ReporteVentasService reporteVentasService;
    private final ReportePdfService reportePdfService;

    @GetMapping("/reportes")
    public String mostrar(
            @RequestParam(name = "fechaInicio", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) java.time.LocalDate fechaInicio,
            @RequestParam(name = "fechaFin", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) java.time.LocalDate fechaFin,
            Model model) {
        agregarReporteAlModelo(reporteVentasService.generar(fechaInicio, fechaFin), model);
        return "reportes/index";
    }

    @GetMapping("/reportes/pdf")
    public ResponseEntity<byte[]> descargarPdf(
            @RequestParam(name = "fechaInicio", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) java.time.LocalDate fechaInicio,
            @RequestParam(name = "fechaFin", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) java.time.LocalDate fechaFin) {
        ReporteVentas reporte = reporteVentasService.generar(fechaInicio, fechaFin);
        if (!reporte.rangoValido()) {
            return ResponseEntity.badRequest().build();
        }

        byte[] pdf = reportePdfService.generar(reporte);
        String nombreArchivo = "reporte-" + reporte.fechaInicio() + "-" + reporte.fechaFin() + ".pdf";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + nombreArchivo + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    private void agregarReporteAlModelo(ReporteVentas reporte, Model model) {
        model.addAttribute("fechaInicio", reporte.fechaInicio());
        model.addAttribute("fechaFin", reporte.fechaFin());
        model.addAttribute("rangoValido", reporte.rangoValido());
        model.addAttribute("totalVentas", reporte.totalVentas());
        model.addAttribute("totalFacturas", reporte.totalFacturas());
        model.addAttribute("ingresos", reporte.ingresos());
        model.addAttribute("variacionVentas", reporte.variacionVentas());
        model.addAttribute("variacionFacturas", reporte.variacionFacturas());
        model.addAttribute("variacionIngresos", reporte.variacionIngresos());
        model.addAttribute("productosMasVendidos", reporte.productosMasVendidos());
        model.addAttribute("cantidadCategorias", reporte.cantidadCategorias());
        model.addAttribute("cantidadClientes", reporte.cantidadClientes());
        model.addAttribute("cantidadProductos", reporte.cantidadProductos());
        model.addAttribute("cantidadFacturas", reporte.cantidadFacturas());
        model.addAttribute("cantidadAnuladas", reporte.cantidadAnuladas());
    }
}
