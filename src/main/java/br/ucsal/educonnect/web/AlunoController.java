package br.ucsal.educonnect.web;

import br.ucsal.educonnect.model.CategoriaDenuncia;
import br.ucsal.educonnect.model.Denuncia;
import br.ucsal.educonnect.security.UsuarioLogado;
import br.ucsal.educonnect.service.DenunciaService;
import br.ucsal.educonnect.service.DesempenhoService;
import br.ucsal.educonnect.model.Nota;
import br.ucsal.educonnect.service.LinhaDesempenho;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;
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
        var linhas = desempenho.desempenho(logado.getId(), selecionado);
        model.addAttribute("linhas", linhas);
        model.addAttribute("mediaGeral", selecionado == null ? null : desempenho.calcularMedia(logado.getId(), selecionado));
        // Pedidos do cliente (validação do RF07): notas de cada avaliação e gráfico de evolução.
        model.addAttribute("notas", notasPorTurma(logado.getId(), linhas));
        model.addAttribute("evolucao", selecionado == null ? List.of() : desempenho.evolucao(logado.getId(), selecionado));
        return "aluno/desempenho";
    }

    /** RF08 - histórico completo com notas e resultados, agrupado por período (pedido do cliente). */
    @GetMapping("/historico")
    public String historico(@AuthenticationPrincipal UsuarioLogado logado, Model model) {
        var linhas = desempenho.historico(logado.getId());
        Map<String, List<LinhaDesempenho>> porPeriodo = new LinkedHashMap<>();
        linhas.forEach(l -> porPeriodo.computeIfAbsent(l.periodo(), p -> new ArrayList<>()).add(l));
        model.addAttribute("linhas", linhas);
        model.addAttribute("porPeriodo", porPeriodo);
        model.addAttribute("notas", notasPorTurma(logado.getId(), linhas));
        model.addAttribute("geradoEm", LocalDateTime.now());
        return "aluno/historico";
    }

    private Map<Long, List<Nota>> notasPorTurma(Long alunoId, List<LinhaDesempenho> linhas) {
        Map<Long, List<Nota>> mapa = new HashMap<>();
        linhas.forEach(l -> mapa.put(l.turmaId(), desempenho.notasDaTurma(alunoId, l.turmaId())));
        return mapa;
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
                            @RequestParam String descricao,
                            @RequestParam(defaultValue = "false") boolean anonima,
                            @RequestParam(name = "anexos", required = false) List<MultipartFile> anexos,
                            RedirectAttributes ra) throws IOException {
        List<DenunciaService.ArquivoEnviado> arquivos = new ArrayList<>();
        if (anexos != null) {
            for (MultipartFile f : anexos) {
                if (f != null && !f.isEmpty()) {
                    arquivos.add(new DenunciaService.ArquivoEnviado(f.getOriginalFilename(), f.getContentType(), f.getBytes()));
                }
            }
        }
        Denuncia d = denuncias.registrar(logado.getId(), titulo, categoria, descricao, anonima, arquivos);
        ra.addFlashAttribute("sucesso", "Denúncia registrada. Guarde o seu protocolo: " + d.getProtocolo());
        return "redirect:/aluno/denuncias/" + d.getId();
    }

    @GetMapping("/denuncias/{id}")
    public String acompanhar(@PathVariable Long id, @AuthenticationPrincipal UsuarioLogado logado, Model model) {
        model.addAttribute("denuncia", denuncias.doAluno(logado.getId(), id));
        return "aluno/denuncia";
    }

    @GetMapping("/anexos/{id}")
    public ResponseEntity<byte[]> baixarAnexo(@PathVariable Long id, @AuthenticationPrincipal UsuarioLogado logado) {
        return Downloads.anexo(denuncias.anexoDoAluno(logado.getId(), id));
    }
}
