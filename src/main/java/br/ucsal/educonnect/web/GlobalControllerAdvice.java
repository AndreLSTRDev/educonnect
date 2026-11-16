package br.ucsal.educonnect.web;

import br.ucsal.educonnect.exception.RegraNegocioException;
import br.ucsal.educonnect.security.UsuarioLogado;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.servlet.support.RequestContextUtils;

@ControllerAdvice
public class GlobalControllerAdvice {

    private static final Logger log = LoggerFactory.getLogger(GlobalControllerAdvice.class);

    /** Disponibiliza o usuário logado (nome/perfil) para o menu de todas as páginas. */
    @ModelAttribute("logado")
    public UsuarioLogado logado(@AuthenticationPrincipal UsuarioLogado usuario) {
        return usuario;
    }

    /**
     * Regras de negócio violadas voltam para a página anterior com a mensagem de erro (RNF-U.4).
     * Só o caminho do Referer é reaproveitado, evitando redirecionamento para sites externos.
     */
    @ExceptionHandler(RegraNegocioException.class)
    public String regraNegocio(RegraNegocioException e, HttpServletRequest request) {
        log.warn("Regra de negócio: {}", e.getMessage());
        RequestContextUtils.getOutputFlashMap(request).put("erro", e.getMessage());
        return "redirect:" + destinoDeRetorno(request);
    }

    private String destinoDeRetorno(HttpServletRequest request) {
        String referer = request.getHeader("Referer");
        if (referer != null) {
            try {
                URI uri = URI.create(referer);
                String caminho = uri.getRawPath();
                if (caminho != null && caminho.startsWith("/") && !caminho.startsWith("//")) {
                    return uri.getRawQuery() == null ? caminho : caminho + "?" + uri.getRawQuery();
                }
            } catch (IllegalArgumentException ignorado) {
                // Referer inválido: usa o destino padrão abaixo.
            }
        }
        return "/painel";
    }
}
