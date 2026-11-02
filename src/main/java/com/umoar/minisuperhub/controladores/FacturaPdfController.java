package com.umoar.minisuperhub.controladores;

import com.umoar.minisuperhub.modelos.Factura;
import com.umoar.minisuperhub.servicios.FacturaExcelService;
import com.umoar.minisuperhub.servicios.FacturaPdfService;
import com.umoar.minisuperhub.servicios.FacturaService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.server.ResponseStatusException;

@Controller
@RequestMapping("/facturas")
@RequiredArgsConstructor
public class FacturaPdfController {

    private final FacturaService facturaService;
    private final FacturaPdfService facturaPdfService;
    private final FacturaExcelService facturaExcelService;

    @Value("${app.minisuper.nombre:Minisúper}")
    private String nombreMinisuper;

    @Value("${app.minisuper.direccion:Dirección del minisúper}")
    private String direccionMinisuper;

    @Value("${app.minisuper.telefono:Teléfono del minisúper}")
    private String telefonoMinisuper;

    @GetMapping("/imprimir/{id}")
    public String imprimirTicket(@PathVariable Long id, Authentication authentication, Model model) {
        Factura factura = buscarFacturaVisible(id, authentication);
        model.addAttribute("factura", factura);
        model.addAttribute("nombreMinisuper", nombreMinisuper);
        model.addAttribute("direccionMinisuper", direccionMinisuper);
        model.addAttribute("telefonoMinisuper", telefonoMinisuper);
        return "facturas/ticket";
    }

    @GetMapping("/descargar/{id}")
    public ResponseEntity<byte[]> descargar(@PathVariable Long id, Authentication authentication) {
        Factura factura = buscarFacturaVisible(id, authentication);
        byte[] pdf = facturaPdfService.generar(factura);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"factura-" + id + ".pdf\"")
                .body(pdf);
    }

    @GetMapping("/descargar-excel/{id}")
    public ResponseEntity<byte[]> descargarExcel(@PathVariable Long id, Authentication authentication) {
        Factura factura = buscarFacturaVisible(id, authentication);
        byte[] excel = facturaExcelService.generar(factura);
        MediaType tipoExcel = MediaType.parseMediaType(
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");

        return ResponseEntity.ok()
                .contentType(tipoExcel)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"factura-" + id + ".xlsx\"")
                .body(excel);
    }

    private Factura buscarFacturaVisible(Long id, Authentication authentication) {
        boolean admin = authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));
        return (admin
                ? facturaService.buscarCompletaPorId(id)
                : facturaService.buscarCompletaPorIdYVendedor(id, authentication.getName()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }
}
