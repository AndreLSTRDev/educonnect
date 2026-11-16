package br.ucsal.educonnect.web;

import br.ucsal.educonnect.model.CategoriaDenuncia;
import br.ucsal.educonnect.model.Denuncia;
import br.ucsal.educonnect.security.UsuarioLogado;
import br.ucsal.educonnect.service.DenunciaService;
import br.ucsal.educonnect.service.DesempenhoService;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Painel do Aluno: RF06 (consulta), RF07, RF08 e RF09. Cada aluno só vê os próprios dados. */
@Controller
@RequestMapping("/aluno")
public class AlunoController {

    private final DesempenhoService desempenho;
    private final DenunciaService denuncias;

    public AlunoController(DesempenhoService desempenho, DenunciaService denuncias) {
        this.desempenho = desempenho;
        this.denuncias = denuncias;
    }

    @GetMapping
    public String home(@AuthenticationPrincipal UsuarioLogado logado, Model model) {
        List<String> periodos = desempenho.periodos(logado.getId());
        model.addAttribute("periodoAtual", periodos.isEmpty() ? null : periodos.get(0));
        model.addAttribute("totalDenuncias", denuncias.doAluno(logado.getId()).size());
        return "aluno/home";
    }

    /** RF07 - médias, frequência e situação de cada disciplina do período selecionado. */
    @GetMapping("/desempenho")
    public String desempenho(@RequestParam(required = false) String periodo,
                             @AuthenticationPrincipal UsuarioLogado logado, Model model) {
        List<String> periodos = desempenho.periodos(logado.getId());
        String selecionado = (periodo == null || periodo.isBlank()) && !periodos.isEmpty() ? periodos.get(0) : periodo;
        model.addAttribute("periodos", periodos);
        model.addAttribute("periodo", selecionado);
        model.addAttribute("linhas", desempenho.desempenho(logado.getId(), selecionado));
        model.addAttribute("mediaGeral", selecionado == null ? null : desempenho.calcularMedia(logado.getId(), selecionado));
        return "aluno/desempenho";
    }

    /** RF08 - histórico completo com notas e resultados. */
    @GetMapping("/historico")
    public String historico(@AuthenticationPrincipal UsuarioLogado logado, Model model) {
        var linhas = desempenho.historico(logado.getId());
        model.addAttribute("linhas", linhas);
        model.addAttribute("notas", linhas.stream().collect(java.util.stream.Collectors.toMap(
                l -> l.turmaId(), l -> desempenho.notasDaTurma(logado.getId(), l.turmaId()))));
        return "aluno/historico";
    }

    // ---------- Denúncias (RF09) ----------

    @GetMapping("/denuncias")
    public String listarDenuncias(@AuthenticationPrincipal UsuarioLogado logado, Model model) {
        model.addAttribute("denuncias", denuncias.doAluno(logado.getId()));
        model.addAttribute("categorias", CategoriaDenuncia.values());
        return "aluno/denuncias";
    }

    @PostMapping("/denuncias")
    public String registrar(@AuthenticationPrincipal UsuarioLogado logado, @RequestParam String titulo,
                            @RequestParam(required = false) CategoriaDenuncia categoria,
                            @RequestParam String descricao, RedirectAttributes ra) {
        Denuncia d = denuncias.registrar(logado.getId(), titulo, categoria, descricao);
        ra.addFlashAttribute("sucesso", "Denúncia registrada. Guarde o seu protocolo: " + d.getProtocolo());
        return "redirect:/aluno/denuncias/" + d.getId();
    }

    @GetMapping("/denuncias/{id}")
    public String acompanhar(@PathVariable Long id, @AuthenticationPrincipal UsuarioLogado logado, Model model) {
        model.addAttribute("denuncia", denuncias.doAluno(logado.getId(), id));
        return "aluno/denuncia";
    }
}
