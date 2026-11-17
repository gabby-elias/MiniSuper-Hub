package com.umoar.minisuperhub.controladores;

import com.umoar.minisuperhub.modelos.DetalleFactura;
import com.umoar.minisuperhub.modelos.Factura;
import com.umoar.minisuperhub.modelos.Producto;
import com.umoar.minisuperhub.servicios.DetalleFacturaService;
import com.umoar.minisuperhub.servicios.FacturaService;
import com.umoar.minisuperhub.servicios.ProductoService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/detalles-factura")
@RequiredArgsConstructor
public class DetalleFacturaController {

    private final DetalleFacturaService detalleFacturaService;
    private final FacturaService facturaService;
    private final ProductoService productoService;

    @Value("${app.minisuper.nombre:Minisúper}")
    private String nombreMinisuper;

    @Value("${app.minisuper.direccion:Dirección del minisúper}")
    private String direccionMinisuper;

    @Value("${app.minisuper.telefono:Teléfono del minisúper}")
    private String telefonoMinisuper;

    @GetMapping
    public String listar(@RequestParam(required = false) String vendedor,
                         Authentication authentication, Model model) {
        boolean admin = authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));
        List<Factura> facturas;
        String vendedorSeleccionado = "";

        if (admin) {
            var resumenVendedores = facturaService.resumirFacturasPorVendedor();
            vendedorSeleccionado = resumenVendedores.stream()
                    .filter(resumen -> resumen.getUsername().equals(vendedor))
                    .map(resumen -> resumen.getUsername())
                    .findFirst()
                    .orElse("");
            facturas = vendedorSeleccionado.isEmpty()
                    ? facturaService.listar()
                    : facturaService.listarPorVendedor(vendedorSeleccionado);
            model.addAttribute("resumenVendedores", resumenVendedores);
        } else {
            facturas = facturaService.listarPorVendedor(authentication.getName());
        }

        model.addAttribute("facturas", facturas);
        model.addAttribute("vendedorSeleccionado", vendedorSeleccionado);
        model.addAttribute("esAdmin", admin);
        return "detalles-factura/lista-facturas";
    }

    @GetMapping("/{id:[0-9]+}")
    public String verFactura(@PathVariable Long id, Authentication authentication, Model model) {
        boolean admin = authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));
        Factura factura = (admin
                ? facturaService.buscarCompletaPorId(id)
                : facturaService.buscarCompletaPorIdYVendedor(id, authentication.getName()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        model.addAttribute("factura", factura);
        model.addAttribute("productos", productoService.listar());
        model.addAttribute("nombreMinisuper", nombreMinisuper);
        model.addAttribute("direccionMinisuper", direccionMinisuper);
        model.addAttribute("telefonoMinisuper", telefonoMinisuper);
        model.addAttribute("seccionActiva", "detalles");
        return "facturas/detalle";
    }

    @GetMapping("/nuevo")
    public String nuevo(Model model) {
        model.addAttribute("facturas", facturaService.listar());
        return "detalles-factura/lista-generadas";
    }

    @GetMapping("/editar/{id}")
    public String editar(@PathVariable Long id, Model model) {
        return detalleFacturaService.buscarPorId(id)
                .map(detalle -> {
                    model.addAttribute("detalleFactura", detalle);
                    cargarOpciones(model);
                    return "detalles-factura/formulario";
                })
                .orElse("redirect:/detalles-factura");
    }

    @PostMapping("/guardar")
    public String guardar(@ModelAttribute("detalleFactura") DetalleFactura detalleFactura,
                          RedirectAttributes redirectAttributes) {
        if (detalleFactura.getFactura() != null) {
            detalleFactura.setFactura(facturaService.buscarPorId(detalleFactura.getFactura().getId()).orElse(null));
        }
        if (detalleFactura.getProducto() != null) {
            detalleFactura.setProducto(productoService.buscarPorId(detalleFactura.getProducto().getId()).orElse(null));
        }
        boolean nuevo = detalleFactura.getId() == 0;
        if (nuevo) {
            detalleFacturaService.guardar(detalleFactura);
        } else {
            detalleFacturaService.actualizar(detalleFactura.getId(), detalleFactura);
        }
        redirectAttributes.addFlashAttribute("alertType", "success");
        redirectAttributes.addFlashAttribute("alertMessage", nuevo
                ? "Detalle guardado correctamente."
                : "Detalle actualizado correctamente.");
        return "redirect:/detalles-factura";
    }

    @PostMapping("/eliminar/{id}")
    public String eliminar(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        detalleFacturaService.eliminar(id);
        redirectAttributes.addFlashAttribute("alertType", "success");
        redirectAttributes.addFlashAttribute("alertMessage", "Detalle eliminado correctamente.");
        return "redirect:/detalles-factura";
    }

    private void cargarOpciones(Model model) {
        model.addAttribute("facturas", facturaService.listar());
        model.addAttribute("productos", productoService.listar());
    }
}
