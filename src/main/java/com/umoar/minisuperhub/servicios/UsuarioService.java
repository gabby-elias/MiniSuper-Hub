package com.umoar.minisuperhub.servicios;

import com.umoar.minisuperhub.dto.UsuarioForm;
import com.umoar.minisuperhub.dto.UsuarioResumen;
import com.umoar.minisuperhub.modelos.Rol;
import com.umoar.minisuperhub.modelos.Usuario;
import com.umoar.minisuperhub.repositorios.RolRepository;
import com.umoar.minisuperhub.repositorios.UsuarioRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class UsuarioService {

    private static final String ROL_ADMIN = "ROLE_ADMIN";

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public List<UsuarioResumen> listar() {
        return usuarioRepository.findAllByOrderByNombreAsc().stream()
                .map(usuario -> new UsuarioResumen(usuario.getId(), usuario.getUsername(), usuario.getNombre(),
                        usuario.getRoles().stream().map(Rol::getNombre).sorted().findFirst()
                                .map(UsuarioService::nombreVisible).orElse(""),
                        usuario.isActivo()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<Rol> listarRoles() {
        return rolRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Usuario buscar(Long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("No se encontró el usuario con id " + id));
    }

    @Transactional
    public Usuario guardar(UsuarioForm form, String usernameActual) {
        boolean nuevo = form.getId() == null || form.getId() == 0;
        Usuario usuario = nuevo ? new Usuario() : buscar(form.getId());
        if (form.getUsername() == null || form.getUsername().isBlank()
                || form.getNombre() == null || form.getNombre().isBlank()
                || form.getRol() == null || form.getRol().isBlank()) {
            throw new IllegalArgumentException("Completa el usuario, nombre y rol.");
        }

        Rol rolSeleccionado = rolRepository.findByNombre(form.getRol().trim())
                .orElseThrow(() -> new IllegalArgumentException("Selecciona un rol válido."));
        String username = form.getUsername().trim();
        if (nuevo ? usuarioRepository.existsByUsername(username)
                : usuarioRepository.existsByUsernameAndIdNot(username, usuario.getId())) {
            throw new IllegalArgumentException("Ese nombre de usuario ya está en uso.");
        }

        boolean esActual = !nuevo && usuario.getUsername().equals(usernameActual);
        if (esActual && !tieneUnicoRol(usuario, rolSeleccionado.getNombre())) {
            throw new AccessDeniedException("No puedes cambiar tu propio rol.");
        }
        if (esActual && !form.isActivo()) {
            throw new AccessDeniedException("No puedes desactivar tu propia cuenta.");
        }
        boolean quitandoAdmin = tieneRol(usuario, ROL_ADMIN)
                && (!ROL_ADMIN.equals(rolSeleccionado.getNombre()) || !form.isActivo());
        if (usuario.isActivo() && quitandoAdmin
                && usuarioRepository.countActivosPorRol(ROL_ADMIN) <= 1) {
            throw new IllegalStateException("Debe permanecer al menos un administrador activo.");
        }

        usuario.setUsername(username);
        usuario.setNombre(form.getNombre().trim());
        usuario.setRoles(Set.of(rolSeleccionado));
        usuario.setActivo(form.isActivo());
        if (nuevo && (form.getPassword() == null || form.getPassword().isBlank())) {
            throw new IllegalArgumentException("La contraseña es obligatoria para un usuario nuevo.");
        }
        if (form.getPassword() != null && !form.getPassword().isBlank()) {
            usuario.setPassword(passwordEncoder.encode(form.getPassword()));
        }
        return usuarioRepository.save(usuario);
    }

    @Transactional
    public void cambiarEstado(Long id, boolean activo, String usernameActual) {
        Usuario usuario = buscar(id);
        if (usuario.getUsername().equals(usernameActual) && !activo) {
            throw new AccessDeniedException("No puedes desactivar tu propia cuenta.");
        }
        if (usuario.isActivo() && !activo && tieneRol(usuario, ROL_ADMIN)
                && usuarioRepository.countActivosPorRol(ROL_ADMIN) <= 1) {
            throw new IllegalStateException("Debe permanecer al menos un administrador activo.");
        }
        usuario.setActivo(activo);
        usuarioRepository.save(usuario);
    }

    private static boolean tieneRol(Usuario usuario, String nombreRol) {
        return usuario.getRoles().stream().anyMatch(rol -> nombreRol.equals(rol.getNombre()));
    }

    private static boolean tieneUnicoRol(Usuario usuario, String nombreRol) {
        return usuario.getRoles().size() == 1 && tieneRol(usuario, nombreRol);
    }

    private static String nombreVisible(String nombreRol) {
        return nombreRol.startsWith("ROLE_") ? nombreRol.substring("ROLE_".length()) : nombreRol;
    }
}
