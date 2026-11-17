package br.ucsal.educonnect.web;

import br.ucsal.educonnect.model.CategoriaDenuncia;
import br.ucsal.educonnect.model.StatusDenuncia;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import br.ucsal.educonnect.security.UsuarioLogado;
import br.ucsal.educonnect.service.DenunciaService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Painel do Gestor de Denúncias: RF10. */
@Controller
@RequestMapping("/gestor")
public class GestorController {

    private final DenunciaService denuncias;

    public GestorController(DenunciaService denuncias) {
        this.denuncias = denuncias;
    }

    /** RF10 com os filtros pedidos pelo cliente: status, categoria, período, aluno e protocolo. */
    @GetMapping
    public String home(@RequestParam(required = false) StatusDenuncia status,
                       @RequestParam(required = false) CategoriaDenuncia categoria,
                       @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate de,
                       @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ate,
                       @RequestParam(required = false) String aluno,
                       @RequestParam(required = false) String protocolo, Model model) {
        var filtro = new DenunciaService.FiltroDenuncia(status, categoria, de, ate, aluno, protocolo);
        model.addAttribute("denuncias", denuncias.listar(filtro));
        model.addAttribute("filtro", filtro);
        model.addAttribute("statusFiltro", status);
        model.addAttribute("todosStatus", StatusDenuncia.values());
        model.addAttribute("categorias", CategoriaDenuncia.values());
        model.addAttribute("filtrando", status != null || categoria != null || de != null || ate != null
                || (aluno != null && !aluno.isBlank()) || (protocolo != null && !protocolo.isBlank()));
        return "gestor/home";
    }

    @GetMapping("/anexos/{id}")
    public ResponseEntity<byte[]> baixarAnexo(@PathVariable Long id) {
        return Downloads.anexo(denuncias.anexo(id));
    }

    @GetMapping("/denuncias/{id}")
    public String detalhe(@PathVariable Long id, Model model) {
        model.addAttribute("denuncia", denuncias.buscar(id));
        model.addAttribute("todosStatus", StatusDenuncia.values());
        return "gestor/denuncia";
    }

    @PostMapping("/denuncias/{id}/status")
    public String atualizarStatus(@PathVariable Long id, @RequestParam(required = false) StatusDenuncia status,
                                  @RequestParam(required = false) String comentario,
                                  @AuthenticationPrincipal UsuarioLogado logado, RedirectAttributes ra) {
        denuncias.atualizarStatus(logado.getId(), id, status, comentario);
        ra.addFlashAttribute("sucesso", "Status atualizado e registrado no histórico.");
        return "redirect:/gestor/denuncias/" + id;
    }
}
