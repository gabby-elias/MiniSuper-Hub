package com.umoar.minisuperhub.controladores.api;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@OpenAPIDefinition(
        info = @Info(
                title = "MiniSuper Hub API",
                version = "1.0.0",
                description = "API REST para consultar información del minisúper."
        )
)
@Tag(name = "Minisúper", description = "Información pública del minisúper")
@RestController
@RequestMapping("/api/minisuper")
public class MinisuperRestController {

    @Value("${app.minisuper.nombre:Minisúper}")
    private String nombre;

    @Value("${app.minisuper.direccion:Dirección del minisúper}")
    private String direccion;

    @Value("${app.minisuper.telefono:0000-0000}")
    private String telefono;

    @GetMapping
    @Operation(summary = "Consultar información del minisúper")
    @ApiResponse(
            responseCode = "200",
            description = "Información pública del establecimiento",
            content = @Content(schema = @Schema(implementation = MinisuperResponse.class))
    )
    public MinisuperResponse obtenerInformacion() {
        return new MinisuperResponse(nombre, direccion, telefono);
    }

    @Schema(description = "Datos públicos del establecimiento")
    public record MinisuperResponse(String nombre, String direccion, String telefono) {
    }
}
