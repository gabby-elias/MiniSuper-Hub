package com.umoar.minisuperhub.configuracion;

import com.umoar.minisuperhub.modelos.Rol;
import com.umoar.minisuperhub.modelos.Usuario;
import com.umoar.minisuperhub.repositorios.RolRepository;
import com.umoar.minisuperhub.repositorios.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
@RequiredArgsConstructor
public class empleadosUsersInitializer implements ApplicationRunner {

    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final PasswordEncoder passwordEncoder;
    private final JdbcTemplate jdbcTemplate;

    @Value("${app.demo.admin.username:admin}")
    private String adminUsername;

    @Value("${app.demo.admin.password:admin1234}")
    private String adminPassword;

    @Value("${app.demo.employee.username:empleado}")
    private String employeeUsername;

    @Value("${app.demo.employee.password:empleado}")
    private String employeePassword;

    @Override
    public void run(ApplicationArguments args) {
        Rol admin = crearRolSiNoExiste("ROLE_ADMIN");
        Rol empleado = crearRolSiNoExiste("ROLE_EMPLEADO");

        migrarRolesExistentes();
        crearSiNoExiste(adminUsername, "Administradora", adminPassword, admin);
        crearSiNoExiste(employeeUsername, "Empleado Hector", employeePassword, empleado);
    }

    private Rol crearRolSiNoExiste(String nombre) {
        return rolRepository.findByNombre(nombre)
                .orElseGet(() -> rolRepository.save(Rol.builder().nombre(nombre).build()));
    }

    private void migrarRolesExistentes() {
        Boolean existeColumnaRol = jdbcTemplate.queryForObject(
                "select exists (select 1 from information_schema.columns " +
                        "where table_schema = current_schema() and table_name = 'usuarios' and column_name = 'rol')",
                Boolean.class);
        if (Boolean.TRUE.equals(existeColumnaRol)) {
            jdbcTemplate.update("insert into usuario_roles (usuario_id, rol_id) " +
                    "select u.id, r.id from usuarios u " +
                    "join roles r on r.nombre = concat('ROLE_', u.rol) " +
                    "on conflict (usuario_id, rol_id) do nothing");
            jdbcTemplate.execute("alter table usuarios drop column rol");
        }
    }

    private void crearSiNoExiste(String username, String nombre, String password, Rol rol) {
        Usuario usuario = usuarioRepository.findByUsername(username).orElseGet(() -> Usuario.builder()
                .username(username)
                .nombre(nombre)
                .password(passwordEncoder.encode(password))
                .roles(Set.of(rol))
                .activo(true)
                .build());

        if (!passwordEncoder.matches(password, usuario.getPassword())) {
            usuario.setPassword(passwordEncoder.encode(password));
        }
        usuario.setRoles(Set.of(rol));
        usuarioRepository.save(usuario);
    }
}
