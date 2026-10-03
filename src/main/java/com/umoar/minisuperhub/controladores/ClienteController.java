package com.umoar.minisuperhub.controladores;

import com.umoar.minisuperhub.modelos.Cliente;
import com.umoar.minisuperhub.servicios.ClienteService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/clientes")
@RequiredArgsConstructor
public class ClienteController {

    private final ClienteService clienteService;

    @GetMapping
    public String listar(Model model) {
        model.addAttribute("clientes", clienteService.listar());
        return "clientes/lista";
    }

    @GetMapping("/nuevo")
    public String nuevo(Model model) {
        model.addAttribute("cliente", new Cliente());
        return "clientes/formulario";
    }

    @GetMapping("/editar/{id}")
    public String editar(@PathVariable Long id, Model model) {
        return clienteService.buscarPorId(id)
                .map(cliente -> {
                    model.addAttribute("cliente", cliente);
                    return "clientes/formulario";
                })
                .orElse("redirect:/clientes");
    }

    @PostMapping("/guardar")
    public String guardar(@ModelAttribute Cliente cliente,
                          Authentication authentication,
                          RedirectAttributes redirectAttributes) {
        boolean nuevo = cliente.getId() == 0;
        boolean admin = authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));
        if (!admin && !nuevo) {
            throw new AccessDeniedException("Los empleados solo pueden crear clientes.");
        }
        if (nuevo) {
            clienteService.guardar(cliente);
        } else {
            clienteService.actualizar(cliente.getId(), cliente);
        }
        redirectAttributes.addFlashAttribute("alertType", "success");
        redirectAttributes.addFlashAttribute("alertMessage", nuevo
                ? "Cliente guardado correctamente."
                : "Cliente actualizado correctamente.");
        return "redirect:/clientes";
    }

    @PostMapping("/eliminar/{id}")
    public String eliminar(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        clienteService.eliminar(id);
        redirectAttributes.addFlashAttribute("alertType", "success");
        redirectAttributes.addFlashAttribute("alertMessage", "Cliente eliminado correctamente.");
        return "redirect:/clientes";
    }
}
