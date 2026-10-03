package com.umoar.minisuperhub.controladores;

import com.umoar.minisuperhub.modelos.Factura;
import com.umoar.minisuperhub.servicios.CategoriaService;
import com.umoar.minisuperhub.servicios.ClienteService;
import com.umoar.minisuperhub.servicios.FacturaService;
import com.umoar.minisuperhub.servicios.ProductoService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.Comparator;

@Controller
@RequiredArgsConstructor
public class DashboardController {

    private final CategoriaService categoriaService;
    private final ClienteService clienteService;
    private final ProductoService productoService;
    private final FacturaService facturaService;

    @GetMapping("/dashboard")
    public String mostrarDashboard(Authentication authentication, Model model) {
        boolean admin = authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));
        var facturas = admin ? facturaService.listar() : facturaService.listarPorVendedor(authentication.getName());
        model.addAttribute("cantidadCategorias", categoriaService.listar().size());
        model.addAttribute("cantidadClientes", clienteService.listar().size());
        model.addAttribute("cantidadProductos", productoService.listar().size());
        model.addAttribute("cantidadFacturas", facturas.size());
        model.addAttribute("ultimasFacturas", facturas.stream()
                .sorted(Comparator.comparingLong(Factura::getId).reversed())
                .limit(5)
                .toList());
        return "dashboard/index";
    }
}
