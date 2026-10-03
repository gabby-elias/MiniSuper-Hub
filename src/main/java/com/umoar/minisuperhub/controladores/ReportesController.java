package com.umoar.minisuperhub.controladores;

import com.umoar.minisuperhub.servicios.CategoriaService;
import com.umoar.minisuperhub.servicios.ClienteService;
import com.umoar.minisuperhub.servicios.FacturaService;
import com.umoar.minisuperhub.servicios.ProductoService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class ReportesController {

    private final CategoriaService categoriaService;
    private final ClienteService clienteService;
    private final FacturaService facturaService;
    private final ProductoService productoService;

    @GetMapping("/reportes")
    public String mostrar(Model model) {
        var facturas = facturaService.listar();
        var vigentes = facturas.stream().filter(factura -> !factura.isAnulada()).toList();
        model.addAttribute("cantidadCategorias", categoriaService.listar().size());
        model.addAttribute("cantidadClientes", clienteService.listar().size());
        model.addAttribute("cantidadProductos", productoService.listar().size());
        model.addAttribute("cantidadFacturas", vigentes.size());
        model.addAttribute("cantidadAnuladas", facturas.size() - vigentes.size());
        model.addAttribute("totalVentas", vigentes.stream().mapToDouble(factura -> factura.getTotal()).sum());
        return "reportes/index";
    }
}
