package com.umoar.minisuperhub.controladores;

import com.umoar.minisuperhub.modelos.Cliente;
import com.umoar.minisuperhub.modelos.DetalleFactura;
import com.umoar.minisuperhub.modelos.Factura;
import com.umoar.minisuperhub.modelos.Producto;
import com.umoar.minisuperhub.modelos.Usuario;
import com.umoar.minisuperhub.repositorios.UsuarioRepository;
import com.umoar.minisuperhub.servicios.ClienteService;
import com.umoar.minisuperhub.servicios.FacturaService;
import com.umoar.minisuperhub.servicios.ProductoService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
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

import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("/facturas")
@RequiredArgsConstructor
public class FacturaController {

    private final FacturaService facturaService;
    private final ClienteService clienteService;
    private final ProductoService productoService;
    private final UsuarioRepository usuarioRepository;

    @GetMapping
    public String listar(Authentication authentication, Model model) {
        boolean admin = esAdmin(authentication);
        model.addAttribute("facturas", admin ? facturaService.listar()
                : facturaService.listarPorVendedor(authentication.getName()));
        return "facturas/lista";
    }

    @GetMapping("/nuevo")
    public String nuevo(Model model) {
        Factura factura = new Factura();
        factura.setFecha(java.time.LocalDateTime.now());

        factura.setDetalles(new ArrayList<>());
        model.addAttribute("factura", factura);
        cargarClientes(model);
        return "facturas/formulario";
    }

    @GetMapping("/editar/{id}")
    public String editar(@PathVariable Long id, Model model) {
        return facturaService.buscarPorId(id)
                .filter(factura -> !factura.isAnulada())
                .map(factura -> {
                    model.addAttribute("factura", factura);
                    cargarClientes(model);
                    return "facturas/formulario";
                })
                .orElse("redirect:/facturas");
    }

    @GetMapping("/ver/{id}")
    public String ver(@PathVariable Long id, Authentication authentication, Model model) {
        var factura = (esAdmin(authentication)
                ? facturaService.buscarPorId(id)
                : facturaService.buscarPorIdYVendedor(id, authentication.getName()));
        return factura.map(item -> {
                    model.addAttribute("factura", item);
                    return "facturas/detalle";
                })
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    @PostMapping("/guardar")
    public String guardar(@ModelAttribute Factura factura,
                          @RequestParam(required = false) Long clienteId,
                          @RequestParam(required = false) List<Long> productoIds,
                          @RequestParam(required = false) List<Integer> cantidades,
                          Authentication authentication,
                          RedirectAttributes redirectAttributes) {
        boolean admin = esAdmin(authentication);
        if (!admin && factura.getId() != 0) {
            throw new AccessDeniedException("Los empleados solo pueden crear facturas.");
        }
        if (productoIds == null || cantidades == null || productoIds.isEmpty()
                || productoIds.size() != cantidades.size()) {
            return volverAlFormulario(factura, redirectAttributes,
                    "Agrega al menos un producto a la factura.");
        }

        Cliente cliente = null;
        if (clienteId != null) {
            cliente = clienteService.buscarPorId(clienteId).orElse(null);
            if (cliente == null) {
                return volverAlFormulario(factura, redirectAttributes,
                        "Selecciona un cliente válido o consumidor final.");
            }
        }

        List<DetalleFactura> detalles = new ArrayList<>();
        double total = 0;
        for (int i = 0; i < productoIds.size(); i++) {
            Integer cantidad = cantidades.get(i);
            Producto producto = productoService.buscarPorId(productoIds.get(i)).orElse(null);
            if (producto == null || cantidad == null || cantidad < 1) {
                return volverAlFormulario(factura, redirectAttributes,
                        "Revisa los productos y cantidades de la factura.");
            }

            double subtotal = producto.getPrecioVenta() * cantidad;
            DetalleFactura detalle = DetalleFactura.builder()
                    .producto(producto)
                    .cantidad(cantidad)
                    .precioUnitario(producto.getPrecioVenta())
                    .subtotal(subtotal)
                    .build();
            detalles.add(detalle);
            total += subtotal;
        }

        boolean nueva = factura.getId() == 0;
        factura.setCliente(cliente);
        factura.setTotal(total);
        Usuario vendedor = usuarioRepository.findByUsername(authentication.getName())
                .orElseThrow(() -> new EntityNotFoundException("No se encontró el usuario autenticado."));
        facturaService.guardarConDetalles(factura, detalles, vendedor);
        redirectAttributes.addFlashAttribute("alertType", "success");
        redirectAttributes.addFlashAttribute("alertMessage", nueva
                ? "Factura guardada correctamente."
                : "Factura actualizada correctamente.");
        return "redirect:/facturas";
    }

    @PostMapping("/anular/{id}")
    public String anular(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        facturaService.anular(id);
        redirectAttributes.addFlashAttribute("alertType", "success");
        redirectAttributes.addFlashAttribute("alertMessage", "Factura anulada correctamente.");
        return "redirect:/facturas";
    }

    @PostMapping("/eliminar/{id}")
    public String eliminar(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        facturaService.eliminar(id);
        redirectAttributes.addFlashAttribute("alertType", "success");
        redirectAttributes.addFlashAttribute("alertMessage", "Factura eliminada correctamente.");
        return "redirect:/facturas";
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
