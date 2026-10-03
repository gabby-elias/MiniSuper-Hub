package com.umoar.minisuperhub.configuracion;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers("/", "/login", "/css/**", "/js/**", "/images/**", "/webjars/**", "/error").permitAll()
                        .requestMatchers("/usuarios/**", "/reportes/**", "/detalles-factura/**").hasRole("ADMIN")
                        .requestMatchers("/facturas/anular/**", "/facturas/editar/**", "/facturas/eliminar/**").hasRole("ADMIN")
                        .requestMatchers("/categorias/nuevo", "/categorias/editar/**", "/productos/nuevo", "/productos/editar/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/categorias/**", "/productos/**").hasRole("ADMIN")
                        .requestMatchers("/clientes/editar/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/clientes/eliminar/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/clientes/guardar").hasAnyRole("ADMIN", "EMPLEADO")
                        .requestMatchers("/categorias/**", "/productos/**", "/clientes/**", "/facturas/**")
                        .hasAnyRole("ADMIN", "EMPLEADO")
                        .anyRequest().authenticated())
                .formLogin(form -> form
                        .loginPage("/login")
                        .loginProcessingUrl("/login")
                        .defaultSuccessUrl("/dashboard", true)
                        .failureUrl("/login?error")
                        .permitAll())
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?logout")
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID")
                        .permitAll());
        return http.build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
