package com.umoar.minisuperhub.controladores;


import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class LoginController {


    @GetMapping({"/", "/login"})
    public String mostrarLogin(Authentication authentication,
                               @RequestParam(required = false) String error,
                               @RequestParam(required = false) String logout,
                               Model model) {
        if (authentication != null && authentication.isAuthenticated()
                && !"anonymousUser".equals(authentication.getPrincipal())) {
            return "redirect:/dashboard";
        }
        model.addAttribute("loginError", error != null);
        model.addAttribute("logout", logout != null);

        return "login";
    }
}
