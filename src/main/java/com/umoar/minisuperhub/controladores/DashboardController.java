package com.umoar.minisuperhub.controladores;

import com.umoar.minisuperhub.modelos.Factura;
import com.umoar.minisuperhub.modelos.Producto;

import com.umoar.minisuperhub.servicios.ClienteService;
import com.umoar.minisuperhub.servicios.FacturaService;
import com.umoar.minisuperhub.servicios.ProductoService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Comparator;

@Controller
@RequiredArgsConstructor
public class DashboardController {


    private final ClienteService clienteService;
    private final ProductoService productoService;
    private final FacturaService facturaService;

    @GetMapping("/dashboard")
    public String mostrarDashboard(Authentication authentication, Model model) {
        boolean admin = authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));
        var facturas = admin ? facturaService.listar() : facturaService.listarPorVendedor(authentication.getName());
        var productos = productoService.listar();
        LocalDate hoy = LocalDate.now(ZoneId.of("America/El_Salvador"));

        model.addAttribute("fechaHoy", hoy);
        model.addAttribute("ventasHoy", facturas.stream()
                .filter(factura -> !factura.isAnulada() && factura.getFecha() != null
                        && factura.getFecha().toLocalDate().equals(hoy))
                .mapToDouble(Factura::getTotal)
                .sum());
        model.addAttribute("cantidadClientes", clienteService.listar().size());
        model.addAttribute("cantidadProductos", productos.size());
        model.addAttribute("cantidadFacturas", facturas.size());
        model.addAttribute("cantidadStockBajo", productos.stream().filter(producto -> producto.getStock() <= 5).count());
        model.addAttribute("productosStockBajo", productos.stream()
                .filter(producto -> producto.getStock() <= 5)
                .sorted(Comparator.comparingInt(Producto::getStock))
                .limit(5)
                .toList());
        model.addAttribute("ultimasFacturas", facturas.stream()
                .sorted(Comparator.comparingLong(Factura::getId).reversed())
                .limit(5)
                .toList());
        return "dashboard/index";
    }
}
