package br.ucsal.educonnect.web;

import br.ucsal.educonnect.exception.RegraNegocioException;
import br.ucsal.educonnect.model.Avaliacao;
import br.ucsal.educonnect.model.Turma;
import br.ucsal.educonnect.security.UsuarioLogado;
import br.ucsal.educonnect.service.AvaliacaoService;
import br.ucsal.educonnect.service.CadastroService;
import br.ucsal.educonnect.service.FrequenciaService;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** Painel do Professor: RF05 (notas) e RF06 (frequência). */
@Controller
@RequestMapping("/professor")
public class ProfessorController {

    private final CadastroService cadastro;
    private final AvaliacaoService avaliacoes;
    private final FrequenciaService frequencia;

    public ProfessorController(CadastroService cadastro, AvaliacaoService avaliacoes, FrequenciaService frequencia) {
        this.cadastro = cadastro;
        this.avaliacoes = avaliacoes;
        this.frequencia = frequencia;
    }

    @GetMapping
    public String home(@AuthenticationPrincipal UsuarioLogado logado, Model model) {
        model.addAttribute("turmas", cadastro.turmasDoProfessor(logado.getId()));
        return "professor/home";
    }

    @GetMapping("/turmas/{id}")
    public String turma(@PathVariable Long id, @AuthenticationPrincipal UsuarioLogado logado, Model model) {
        Turma turma = cadastro.turmaDoProfessor(logado.getId(), id);
        model.addAttribute("turma", turma);
        model.addAttribute("avaliacoes", avaliacoes.listarAvaliacoes(id));
        model.addAttribute("hoje", LocalDate.now());
        return "professor/turma";
    }

    @PostMapping("/turmas/{id}/avaliacoes")
    public String cadastrarAvaliacao(@PathVariable Long id, @AuthenticationPrincipal UsuarioLogado logado,
                                     @RequestParam String nome, @RequestParam String data,
                                     @RequestParam String valorMaximo, RedirectAttributes ra) {
        avaliacoes.cadastrarAvaliacao(logado.getId(), id, nome, data, valorMaximo);
        ra.addFlashAttribute("sucesso", "Avaliação cadastrada. Agora lance as notas.");
        return "redirect:/professor/turmas/" + id;
    }

    // ---------- Notas ----------

    @GetMapping("/avaliacoes/{id}/notas")
    public String formNotas(@PathVariable Long id, @AuthenticationPrincipal UsuarioLogado logado, Model model) {
        Avaliacao avaliacao = avaliacoes.avaliacaoDoProfessor(logado.getId(), id);
        model.addAttribute("avaliacao", avaliacao);
        model.addAttribute("turma", avaliacao.getTurma());
        model.addAttribute("notas", avaliacoes.notasPorAluno(id));
        return "professor/notas";
    }

    @PostMapping("/avaliacoes/{id}/notas")
    public String salvarNotas(@PathVariable Long id, @AuthenticationPrincipal UsuarioLogado logado,
                              @RequestParam Map<String, String> params, RedirectAttributes ra) {
        Avaliacao avaliacao = avaliacoes.avaliacaoDoProfessor(logado.getId(), id);
        Map<Long, String> valores = new HashMap<>();
        Map<Long, String> obs = new HashMap<>();
        avaliacao.getTurma().getAlunos().forEach(a -> {
            valores.put(a.getId(), params.get("valor_" + a.getId()));
            obs.put(a.getId(), params.get("obs_" + a.getId()));
        });
        int total = avaliacoes.lancarNotas(logado.getId(), id, valores, obs);
        ra.addFlashAttribute("sucesso", total + " nota(s) salva(s) com sucesso.");
        return "redirect:/professor/avaliacoes/" + id + "/notas";
    }

    // ---------- Frequência ----------

    @GetMapping("/turmas/{id}/frequencia")
    public String formFrequencia(@PathVariable Long id, @RequestParam(required = false) String data,
                                 @AuthenticationPrincipal UsuarioLogado logado, Model model) {
        Turma turma = cadastro.turmaDoProfessor(logado.getId(), id);
        LocalDate dia = parseData(data);
        model.addAttribute("turma", turma);
        model.addAttribute("data", dia);
        model.addAttribute("hoje", LocalDate.now());
        model.addAttribute("presencas", frequencia.presencasDoDia(id, dia));
        model.addAttribute("justificativas", frequencia.justificativasDoDia(id, dia));
        return "professor/frequencia";
    }

    @PostMapping("/turmas/{id}/frequencia")
    public String salvarFrequencia(@PathVariable Long id, @RequestParam String data,
                                   @RequestParam(required = false) List<Long> presentes,
                                   @RequestParam Map<String, String> params,
                                   @AuthenticationPrincipal UsuarioLogado logado, RedirectAttributes ra) {
        LocalDate dia = parseData(data);
        Turma turma = cadastro.turmaDoProfessor(logado.getId(), id);
        // Pedido do cliente (validação do RF06): justificativa de falta por aluno (campo just_<id>).
        Map<Long, String> justificativas = new HashMap<>();
        turma.getAlunos().forEach(a -> justificativas.put(a.getId(), params.get("just_" + a.getId())));
        frequencia.registrar(logado.getId(), id, dia, presentes, justificativas);
        ra.addFlashAttribute("sucesso", "Frequência registrada.");
        return "redirect:/professor/turmas/" + id + "/frequencia?data=" + dia;
    }

    /** Pedido do cliente (validação do RF04): o professor da turma é quem encerra a turma. */
    @PostMapping("/turmas/{id}/encerrar")
    public String encerrar(@PathVariable Long id, @AuthenticationPrincipal UsuarioLogado logado,
                           RedirectAttributes ra) {
        cadastro.encerrarTurma(logado.getId(), id);
        ra.addFlashAttribute("sucesso", "Turma encerrada. Os resultados finais já são exibidos aos alunos.");
        return "redirect:/professor/turmas/" + id;
    }

    private LocalDate parseData(String data) {
        if (data == null || data.isBlank()) {
            return LocalDate.now();
        }
        try {
            return LocalDate.parse(data);
        } catch (DateTimeParseException e) {
            throw new RegraNegocioException("Informe uma data válida.");
        }
    }
}
