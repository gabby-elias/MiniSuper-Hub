package com.umoar.minisuperhub.controladores;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class DocumentacionController {

    @GetMapping("/docs")
    public String mostrarDocumentacion() {
        return "redirect:/docs/index.html";
    }
}
