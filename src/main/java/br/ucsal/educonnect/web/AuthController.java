package br.ucsal.educonnect.web;

import br.ucsal.educonnect.model.Usuario;
import br.ucsal.educonnect.security.UsuarioLogado;
import br.ucsal.educonnect.service.UsuarioService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/** RF01 - Login e redirecionamento ao painel do perfil. */
@Controller
public class AuthController {

    private final UsuarioService usuarios;

    public AuthController(UsuarioService usuarios) {
        this.usuarios = usuarios;
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/")
    public String raiz() {
        return "redirect:/painel";
    }

    /** Polimorfismo: cada perfil decide o seu painel via {@code exibirPainel()}. */
    @GetMapping("/painel")
    public String painel(@AuthenticationPrincipal UsuarioLogado logado) {
        Usuario usuario = usuarios.buscar(logado.getId());
        return "redirect:" + usuario.exibirPainel();
    }
}
