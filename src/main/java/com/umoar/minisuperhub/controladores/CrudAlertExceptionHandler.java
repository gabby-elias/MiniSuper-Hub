package com.umoar.minisuperhub.controladores;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@ControllerAdvice(annotations = Controller.class)
public class CrudAlertExceptionHandler {

    @ExceptionHandler(DataIntegrityViolationException.class)
    public String manejarErrorDeIntegridad(HttpServletRequest request,
                                           RedirectAttributes redirectAttributes) {
        String ruta = request.getServletPath();
        int siguienteSeparador = ruta.indexOf('/', 1);
        String base = siguienteSeparador < 0 ? "/dashboard" : ruta.substring(0, siguienteSeparador);

        redirectAttributes.addFlashAttribute("alertType", "error");
        if (ruta.endsWith("/guardar")) {
            redirectAttributes.addFlashAttribute("alertMessage",
                    "No se pudo guardar. Revisa los datos obligatorios e inténtalo de nuevo.");
            return "redirect:" + base + "/nuevo";
        }
        redirectAttributes.addFlashAttribute("alertMessage",
                "No se pudo eliminar porque el registro está relacionado con otros datos.");
        return "redirect:" + base;
    }
}
