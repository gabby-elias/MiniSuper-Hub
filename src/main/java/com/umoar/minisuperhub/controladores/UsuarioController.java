package com.umoar.minisuperhub.controladores;

import com.umoar.minisuperhub.dto.UsuarioForm;
import com.umoar.minisuperhub.modelos.Usuario;
import com.umoar.minisuperhub.servicios.UsuarioService;
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

@Controller
@RequestMapping("/usuarios")
@RequiredArgsConstructor
public class UsuarioController {

    private final UsuarioService usuarioService;

    @GetMapping
    public String listar(Model model) {
        model.addAttribute("usuarios", usuarioService.listar());
        return "usuarios/lista";
    }

    @GetMapping("/nuevo")
    public String nuevo(Model model) {
        model.addAttribute("usuarioForm", new UsuarioForm());
        model.addAttribute("roles", usuarioService.listarRoles());
        return "usuarios/formulario";
    }

    @GetMapping("/editar/{id}")
    public String editar(@PathVariable Long id, Model model) {
        Usuario usuario = usuarioService.buscar(id);
        UsuarioForm form = new UsuarioForm();
        form.setId(usuario.getId());
        form.setUsername(usuario.getUsername());
        form.setNombre(usuario.getNombre());
        form.setRol(usuario.getRoles().stream().findFirst().map(rol -> rol.getNombre()).orElse(""));
        form.setActivo(usuario.isActivo());
        model.addAttribute("usuarioForm", form);
        model.addAttribute("roles", usuarioService.listarRoles());
        return "usuarios/formulario";
    }

    @PostMapping("/guardar")
    public String guardar(@ModelAttribute UsuarioForm usuarioForm,
                          Authentication authentication,
                          RedirectAttributes redirectAttributes) {
        boolean nuevo = usuarioForm.getId() == null || usuarioForm.getId() == 0;
        try {
            usuarioService.guardar(usuarioForm, authentication.getName());
        } catch (IllegalArgumentException | IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("alertType", "error");
            redirectAttributes.addFlashAttribute("alertMessage", ex.getMessage());
            return nuevo ? "redirect:/usuarios/nuevo" : "redirect:/usuarios/editar/" + usuarioForm.getId();
        }
        redirectAttributes.addFlashAttribute("alertType", "success");
        redirectAttributes.addFlashAttribute("alertMessage", nuevo
                ? "Usuario creado correctamente."
                : "Usuario actualizado correctamente.");
        return "redirect:/usuarios";
    }

    @PostMapping("/{id}/desactivar")
    public String desactivar(@PathVariable Long id, Authentication authentication,
                             RedirectAttributes redirectAttributes) {
        try {
            usuarioService.cambiarEstado(id, false, authentication.getName());
        } catch (IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("alertType", "error");
            redirectAttributes.addFlashAttribute("alertMessage", ex.getMessage());
            return "redirect:/usuarios";
        }
        redirectAttributes.addFlashAttribute("alertType", "success");
        redirectAttributes.addFlashAttribute("alertMessage", "Usuario desactivado correctamente.");
        return "redirect:/usuarios";
    }

    @PostMapping("/{id}/activar")
    public String activar(@PathVariable Long id, Authentication authentication,
                          RedirectAttributes redirectAttributes) {
        try {
            usuarioService.cambiarEstado(id, true, authentication.getName());
        } catch (IllegalStateException ex) {
            redirectAttributes.addFlashAttribute("alertType", "error");
            redirectAttributes.addFlashAttribute("alertMessage", ex.getMessage());
            return "redirect:/usuarios";
        }
        redirectAttributes.addFlashAttribute("alertType", "success");
        redirectAttributes.addFlashAttribute("alertMessage", "Usuario activado correctamente.");
        return "redirect:/usuarios";
    }
}
