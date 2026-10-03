package com.umoar.minisuperhub.controladores;

import com.umoar.minisuperhub.modelos.Categoria;
import com.umoar.minisuperhub.modelos.Producto;
import com.umoar.minisuperhub.servicios.CategoriaService;
import com.umoar.minisuperhub.servicios.FacturaService;
import com.umoar.minisuperhub.servicios.ProductoService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.ZoneId;

@Controller
@RequestMapping("/productos")
@RequiredArgsConstructor
public class ProductoController {

    private final ProductoService productoService;
    private final CategoriaService categoriaService;
    private final FacturaService facturaService;

    @GetMapping
    public String listar(Authentication authentication, Model model) {
        var productos = productoService.listar();
        boolean admin = authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));
        var facturas = admin ? facturaService.listar() : facturaService.listarPorVendedor(authentication.getName());
        LocalDate hoy = LocalDate.now(ZoneId.of("America/El_Salvador"));
        double ventasHoy = facturas.stream()
                .filter(factura -> !factura.isAnulada() && factura.getFecha() != null
                        && factura.getFecha().toLocalDate().equals(hoy))
                .mapToDouble(factura -> factura.getTotal())
                .sum();
        model.addAttribute("productos", productos);
        model.addAttribute("cantidadProductos", productos.size());
        model.addAttribute("cantidadStockBajo", productos.stream().filter(producto -> producto.getStock() <= 5).count());
        model.addAttribute("ventasHoy", ventasHoy);
        return "productos/lista";
    }

    @GetMapping("/nuevo")
    public String nuevo(Model model) {
        Producto producto = new Producto();
        producto.setCategoria(new Categoria());
        model.addAttribute("producto", producto);
        cargarCategorias(model);
        return "productos/formulario";
    }

    @GetMapping("/editar/{id}")
    public String editar(@PathVariable Long id, Model model) {
        return productoService.buscarPorId(id)
                .map(producto -> {
                    model.addAttribute("producto", producto);
                    cargarCategorias(model);
                    return "productos/formulario";
                })
                .orElse("redirect:/productos");
    }

    @PostMapping("/guardar")
    public String guardar(@ModelAttribute Producto producto, RedirectAttributes redirectAttributes) {
        if (producto.getCategoria() != null) {
            producto.setCategoria(categoriaService.buscarPorId(producto.getCategoria().getId()).orElse(null));
        }
        boolean nuevo = producto.getId() == 0;
        if (nuevo) {
            productoService.guardar(producto);
        } else {
            productoService.actualizar(producto.getId(), producto);
        }
        redirectAttributes.addFlashAttribute("alertType", "success");
        redirectAttributes.addFlashAttribute("alertMessage", nuevo
                ? "Producto guardado correctamente."
                : "Producto actualizado correctamente.");
        return "redirect:/productos";
    }

    @PostMapping("/eliminar/{id}")
    public String eliminar(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        productoService.eliminar(id);
        redirectAttributes.addFlashAttribute("alertType", "success");
        redirectAttributes.addFlashAttribute("alertMessage", "Producto eliminado correctamente.");
        return "redirect:/productos";
    }

    private void cargarCategorias(Model model) {
        model.addAttribute("categorias", categoriaService.listar());
    }
}
