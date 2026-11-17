package com.umoar.minisuperhub.controladores;

import com.umoar.minisuperhub.modelos.Factura;
import com.umoar.minisuperhub.servicios.ClienteService;
import com.umoar.minisuperhub.servicios.FacturaService;
import com.umoar.minisuperhub.servicios.ProductoService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("/facturas")
@RequiredArgsConstructor
public class FacturaController {

    private final FacturaService facturaService;
    private final ClienteService clienteService;
    private final ProductoService productoService;

    @Value("${app.minisuper.nombre:Minisúper}")
    private String nombreMinisuper;

    @Value("${app.minisuper.direccion:Dirección del minisúper}")
    private String direccionMinisuper;

    @Value("${app.minisuper.telefono:Teléfono del minisúper}")
    private String telefonoMinisuper;

    @GetMapping
    public String listar(Authentication authentication, Model model) {
        boolean admin = esAdmin(authentication);
        var facturas = admin ? facturaService.listar()
                : facturaService.listarPorVendedor(authentication.getName());
        LocalDate hoy = LocalDate.now(ZoneId.of("America/El_Salvador"));

        model.addAttribute("facturas", facturas);
        model.addAttribute("totalFacturas", facturas.size());
        model.addAttribute("ventasHoy", facturas.stream()
                .filter(factura -> !factura.isAnulada() && factura.getFecha() != null
                        && factura.getFecha().toLocalDate().equals(hoy))
                .mapToDouble(Factura::getTotal)
                .sum());
        model.addAttribute("facturasPendientes", facturas.stream()
                .filter(factura -> !factura.isAnulada() && !factura.isEntregada())
                .count());
        model.addAttribute("facturasAnuladas", facturas.stream().filter(Factura::isAnulada).count());
        return "facturas/lista";
    }

    @GetMapping("/nuevo")
    public String nuevo(Model model) {
        Factura factura = new Factura();
        factura.setFecha(LocalDateTime.now());

        factura.setDetalles(new ArrayList<>());
        model.addAttribute("factura", factura);
        cargarClientes(model);
        return "facturas/formulario";
    }

    @GetMapping("/editar/{id}")
    public String editar(@PathVariable Long id, Authentication authentication, Model model) {
        Factura factura = buscarFacturaAutorizada(id, authentication);
        if (factura.isAnulada()) {
            return "redirect:/facturas";
        }
        model.addAttribute("factura", factura);
        cargarClientes(model);
        return "facturas/formulario";
    }

    @GetMapping("/ver/{id}")
    public String ver(@PathVariable Long id, Authentication authentication, Model model) {
        model.addAttribute("factura", buscarFacturaAutorizada(id, authentication));
        model.addAttribute("productos", productoService.listar());
        model.addAttribute("nombreMinisuper", nombreMinisuper);
        model.addAttribute("direccionMinisuper", direccionMinisuper);
        model.addAttribute("telefonoMinisuper", telefonoMinisuper);
        model.addAttribute("seccionActiva", "facturas");
        return "facturas/detalle";
    }

    @PostMapping("/guardar")
    public String guardar(@ModelAttribute Factura factura,
                          @RequestParam(required = false) Long clienteId,
                          @RequestParam(required = false) List<Long> productoIds,
                          @RequestParam(required = false) List<Integer> cantidades,
                          Authentication authentication,
                          RedirectAttributes redirectAttributes) {
        boolean nueva = factura.getId() == 0;
        try {
            facturaService.guardarDesdeFormulario(factura, clienteId, productoIds, cantidades,
                    authentication.getName(), esAdmin(authentication));
        } catch (IllegalArgumentException | IllegalStateException ex) {
            return volverAlFormulario(factura, redirectAttributes, ex.getMessage());
        }
        redirectAttributes.addFlashAttribute("alertType", "success");
        redirectAttributes.addFlashAttribute("alertMessage", nueva
                ? "Factura guardada correctamente."
                : "Factura actualizada correctamente.");
        return "redirect:/facturas";
    }

    @PostMapping("/agregar-producto/{id}")
    public String agregarProducto(@PathVariable Long id,
                                  @RequestParam Long productoId,
                                  @RequestParam int cantidad,
                                  Authentication authentication,
                                  RedirectAttributes redirectAttributes) {
        Factura factura = buscarFacturaAutorizada(id, authentication);
        if (factura.isAnulada()) {
            redirectAttributes.addFlashAttribute("alertType", "error");
            redirectAttributes.addFlashAttribute("alertMessage", "No se puede modificar una factura anulada.");
            return "redirect:/facturas/ver/" + id;
        }
        try {
            facturaService.agregarProducto(id, productoId, cantidad);
        } catch (EntityNotFoundException ex) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        } catch (IllegalArgumentException | IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("alertType", "error");
            redirectAttributes.addFlashAttribute("alertMessage", ex.getMessage());
            return "redirect:/facturas/ver/" + id;
        }
        redirectAttributes.addFlashAttribute("alertType", "success");
        redirectAttributes.addFlashAttribute("alertMessage", "Producto agregado a la factura.");
        return "redirect:/facturas/ver/" + id;
    }

    @PostMapping("/entregar/{id}")
    public String marcarEntregada(@PathVariable Long id, Authentication authentication,
                                  RedirectAttributes redirectAttributes) {
        Factura factura = buscarFacturaAutorizada(id, authentication);
        try {
            facturaService.marcarEntregada(factura.getId());
        } catch (IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("alertType", "error");
            redirectAttributes.addFlashAttribute("alertMessage", ex.getMessage());
            return "redirect:/facturas";
        }
        redirectAttributes.addFlashAttribute("alertType", "success");
        redirectAttributes.addFlashAttribute("alertMessage", "Factura marcada como entregada.");
        return "redirect:/facturas";
    }

    @PostMapping("/anular/{id}")
    public String anular(@PathVariable Long id, Authentication authentication,
                         RedirectAttributes redirectAttributes) {
        buscarFacturaAutorizada(id, authentication);
        facturaService.anular(id);
        redirectAttributes.addFlashAttribute("alertType", "success");
        redirectAttributes.addFlashAttribute("alertMessage", "Factura anulada correctamente.");
        return "redirect:/facturas";
    }

    @PostMapping("/eliminar/{id}")
    public String eliminar(@PathVariable Long id, Authentication authentication,
                           RedirectAttributes redirectAttributes) {
        buscarFacturaAutorizada(id, authentication);
        facturaService.eliminar(id);
        redirectAttributes.addFlashAttribute("alertType", "success");
        redirectAttributes.addFlashAttribute("alertMessage", "Factura eliminada correctamente.");
        return "redirect:/facturas";
    }

    private Factura buscarFacturaAutorizada(Long id, Authentication authentication) {
        return (esAdmin(authentication)
                ? facturaService.buscarPorId(id)
                : facturaService.buscarPorIdYVendedor(id, authentication.getName()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    private boolean esAdmin(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));
    }

    private void cargarClientes(Model model) {
        model.addAttribute("clientes", clienteService.listar());
        model.addAttribute("productos", productoService.listar());
    }

    private String volverAlFormulario(Factura factura,
                                      RedirectAttributes redirectAttributes,
                                      String mensaje) {
        redirectAttributes.addFlashAttribute("alertType", "error");
        redirectAttributes.addFlashAttribute("alertMessage", mensaje);
        if (factura.getId() == 0) {
            return "redirect:/facturas/nuevo";
        }
        return "redirect:/facturas/editar/" + factura.getId();
    }
}
