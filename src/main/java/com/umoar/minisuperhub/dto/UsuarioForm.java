package com.umoar.minisuperhub.dto;


import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class UsuarioForm {

    private Long id;
    private String username;
    private String nombre;
    private String password;
    private String rol;
    private boolean activo = true;
}
