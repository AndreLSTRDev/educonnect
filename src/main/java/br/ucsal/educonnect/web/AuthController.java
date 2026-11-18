package br.ucsal.educonnect.web;

import br.ucsal.educonnect.model.Usuario;
import br.ucsal.educonnect.security.UsuarioLogado;
import br.ucsal.educonnect.service.SenhaService;
import br.ucsal.educonnect.service.UsuarioService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/** RF01 - Login, redirecionamento ao painel do perfil e "Esqueci minha senha". */
@Controller
public class AuthController {

    private final UsuarioService usuarios;
    private final SenhaService senhas;
    private final String urlBase;

    public AuthController(UsuarioService usuarios, SenhaService senhas,
                          @Value("${educonnect.url-base:}") String urlBase) {
        this.usuarios = usuarios;
        this.senhas = senhas;
        this.urlBase = urlBase;
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

    // ---------- Esqueci minha senha (pedido do cliente na validação do RF01) ----------

    @GetMapping("/recuperar-senha")
    public String recuperarSenha() {
        return "recuperar-senha";
    }

    @PostMapping("/recuperar-senha")
    public String solicitarLink(@RequestParam(required = false) String identificador, RedirectAttributes ra) {
        String base = urlBase.isBlank() ? ServletUriComponentsBuilder.fromCurrentContextPath().toUriString() : urlBase;
        senhas.solicitar(identificador, base);
        // Mesma mensagem com ou sem usuário encontrado, para não revelar quem está cadastrado.
        ra.addFlashAttribute("sucesso", "Se houver um usuário ativo com esses dados, enviamos ao e-mail cadastrado "
                + "um link para criar uma nova senha. O link vale por 30 minutos.");
        return "redirect:/recuperar-senha";
    }

    @GetMapping("/redefinir-senha")
    public String formRedefinir(@RequestParam(required = false) String token, Model model) {
        model.addAttribute("token", token);
        model.addAttribute("tokenValido", senhas.tokenValido(token));
        return "redefinir-senha";
    }

    @PostMapping("/redefinir-senha")
    public String redefinir(@RequestParam String token, @RequestParam String senha,
                            @RequestParam String confirmacao, RedirectAttributes ra) {
        senhas.redefinir(token, senha, confirmacao);
        return "redirect:/login?senhaRedefinida";
    }
}
