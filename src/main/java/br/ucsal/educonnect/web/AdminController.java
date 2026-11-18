package br.ucsal.educonnect.web;

import br.ucsal.educonnect.model.Perfil;
import br.ucsal.educonnect.security.UsuarioLogado;
import br.ucsal.educonnect.service.CadastroService;
import br.ucsal.educonnect.service.UsuarioService;
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

/** Painel do Administrador: RF02, RF03 e RF04. */
@Controller
@RequestMapping("/admin")
public class AdminController {

    private final UsuarioService usuarios;
    private final CadastroService cadastro;

    public AdminController(UsuarioService usuarios, CadastroService cadastro) {
        this.usuarios = usuarios;
        this.cadastro = cadastro;
    }

    @GetMapping
    public String home(Model model) {
        model.addAttribute("totalUsuarios", usuarios.listar().size());
        model.addAttribute("totalCursos", cadastro.listarCursos().size());
        model.addAttribute("totalDisciplinas", cadastro.listarDisciplinas().size());
        model.addAttribute("totalTurmas", cadastro.listarTurmas().size());
        return "admin/home";
    }

    // ---------- Usuários (RF02) ----------

    /** Pedido do cliente (validação do RF02): busca e filtro por perfil. */
    @GetMapping("/usuarios")
    public String listarUsuarios(@RequestParam(required = false) String busca,
                                 @RequestParam(required = false) Perfil perfil, Model model) {
        model.addAttribute("usuarios", usuarios.listar(busca, perfil));
        model.addAttribute("busca", busca);
        model.addAttribute("perfilFiltro", perfil);
        model.addAttribute("perfis", Perfil.values());
        return "admin/usuarios";
    }

    @GetMapping("/usuarios/novo")
    public String novoUsuario(Model model) {
        model.addAttribute("perfis", Perfil.values());
        return "admin/usuario-form";
    }

    @PostMapping("/usuarios")
    public String criarUsuario(@RequestParam Perfil perfil, @RequestParam String nome,
                               @RequestParam String identificador, @RequestParam(required = false) String email,
                               @RequestParam String senha, @RequestParam(required = false) String complemento,
                               RedirectAttributes ra) {
        usuarios.cadastrar(perfil, nome, identificador, email, senha, complemento);
        ra.addFlashAttribute("sucesso", "Usuário cadastrado com sucesso.");
        return "redirect:/admin/usuarios";
    }

    @GetMapping("/usuarios/{id}/editar")
    public String editarUsuario(@PathVariable Long id, Model model) {
        model.addAttribute("usuario", usuarios.buscar(id));
        model.addAttribute("perfis", Perfil.values());
        return "admin/usuario-form";
    }

    @PostMapping("/usuarios/{id}")
    public String atualizarUsuario(@PathVariable Long id, @RequestParam String nome,
                                   @RequestParam String identificador, @RequestParam(required = false) String email,
                                   @RequestParam(required = false) String senha,
                                   @RequestParam(required = false) String complemento, RedirectAttributes ra) {
        usuarios.atualizar(id, nome, identificador, email, senha, complemento);
        ra.addFlashAttribute("sucesso", "Usuário atualizado com sucesso.");
        return "redirect:/admin/usuarios";
    }

    @PostMapping("/usuarios/{id}/desativar")
    public String desativar(@PathVariable Long id, @AuthenticationPrincipal UsuarioLogado logado,
                            RedirectAttributes ra) {
        usuarios.alterarStatus(id, false, logado.getId());
        ra.addFlashAttribute("sucesso", "Usuário desativado.");
        return "redirect:/admin/usuarios";
    }

    @PostMapping("/usuarios/{id}/ativar")
    public String ativar(@PathVariable Long id, @AuthenticationPrincipal UsuarioLogado logado,
                         RedirectAttributes ra) {
        usuarios.alterarStatus(id, true, logado.getId());
        ra.addFlashAttribute("sucesso", "Usuário reativado.");
        return "redirect:/admin/usuarios";
    }

    // ---------- Cursos e disciplinas (RF03) ----------

    @GetMapping("/cursos")
    public String cursos(Model model) {
        model.addAttribute("cursos", cadastro.listarCursos());
        model.addAttribute("disciplinas", cadastro.listarDisciplinas());
        return "admin/cursos";
    }

    @PostMapping("/cursos")
    public String criarCurso(@RequestParam String nome, RedirectAttributes ra) {
        cadastro.criarCurso(nome);
        ra.addFlashAttribute("sucesso", "Curso cadastrado com sucesso.");
        return "redirect:/admin/cursos";
    }

    /** Pedido do cliente (validação do RF03): editar e excluir cursos. */
    @PostMapping("/cursos/{id}")
    public String renomearCurso(@PathVariable Long id, @RequestParam String nome, RedirectAttributes ra) {
        cadastro.renomearCurso(id, nome);
        ra.addFlashAttribute("sucesso", "Curso renomeado.");
        return "redirect:/admin/cursos";
    }

    @PostMapping("/cursos/{id}/excluir")
    public String excluirCurso(@PathVariable Long id, RedirectAttributes ra) {
        cadastro.excluirCurso(id);
        ra.addFlashAttribute("sucesso", "Curso excluído.");
        return "redirect:/admin/cursos";
    }

    @PostMapping("/disciplinas")
    public String criarDisciplina(@RequestParam(required = false) Long cursoId, @RequestParam String nome,
                                  @RequestParam String codigo, @RequestParam String cargaHoraria,
                                  @RequestParam(required = false) String ementa,
                                  @RequestParam(required = false) List<Long> preRequisitos,
                                  RedirectAttributes ra) {
        cadastro.criarDisciplina(cursoId, nome, codigo, cargaHoraria, ementa, preRequisitos);
        ra.addFlashAttribute("sucesso", "Disciplina cadastrada com sucesso.");
        return "redirect:/admin/cursos";
    }

    /** Pedido do cliente (validação do RF03): editar e excluir disciplinas. */
    @GetMapping("/disciplinas/{id}/editar")
    public String editarDisciplina(@PathVariable Long id, Model model) {
        model.addAttribute("disciplina", cadastro.buscarDisciplina(id));
        model.addAttribute("cursos", cadastro.listarCursos());
        model.addAttribute("disciplinas", cadastro.listarDisciplinas());
        return "admin/disciplina-form";
    }

    @PostMapping("/disciplinas/{id}")
    public String atualizarDisciplina(@PathVariable Long id, @RequestParam(required = false) Long cursoId,
                                      @RequestParam String nome, @RequestParam String codigo,
                                      @RequestParam String cargaHoraria,
                                      @RequestParam(required = false) String ementa,
                                      @RequestParam(required = false) List<Long> preRequisitos,
                                      RedirectAttributes ra) {
        cadastro.atualizarDisciplina(id, cursoId, nome, codigo, cargaHoraria, ementa, preRequisitos);
        ra.addFlashAttribute("sucesso", "Disciplina atualizada.");
        return "redirect:/admin/cursos";
    }

    @PostMapping("/disciplinas/{id}/excluir")
    public String excluirDisciplina(@PathVariable Long id, RedirectAttributes ra) {
        cadastro.excluirDisciplina(id);
        ra.addFlashAttribute("sucesso", "Disciplina excluída.");
        return "redirect:/admin/cursos";
    }

    // ---------- Turmas (RF04) ----------

    @GetMapping("/turmas")
    public String turmas(Model model) {
        model.addAttribute("turmas", cadastro.listarTurmas());
        model.addAttribute("disciplinas", cadastro.listarDisciplinas());
        model.addAttribute("professores", cadastro.professoresAtivos());
        return "admin/turmas";
    }

    @PostMapping("/turmas")
    public String criarTurma(@RequestParam(required = false) Long disciplinaId,
                             @RequestParam(required = false) Long professorId,
                             @RequestParam String codigo, @RequestParam String periodo,
                             @RequestParam(required = false) String sala, RedirectAttributes ra) {
        var turma = cadastro.criarTurma(disciplinaId, professorId, codigo, periodo, sala);
        ra.addFlashAttribute("sucesso", "Turma criada. Agora vincule os alunos.");
        return "redirect:/admin/turmas/" + turma.getId();
    }

    @GetMapping("/turmas/{id}")
    public String turma(@PathVariable Long id, Model model) {
        model.addAttribute("turma", cadastro.buscarTurma(id));
        model.addAttribute("alunosDisponiveis", cadastro.alunosAtivos());
        model.addAttribute("professores", cadastro.professoresAtivos());
        return "admin/turma";
    }

    @PostMapping("/turmas/{id}/alunos")
    public String vincularAluno(@PathVariable Long id, @RequestParam(required = false) Long alunoId,
                                RedirectAttributes ra) {
        cadastro.vincularAluno(id, alunoId);
        ra.addFlashAttribute("sucesso", "Aluno vinculado à turma.");
        return "redirect:/admin/turmas/" + id;
    }

    @PostMapping("/turmas/{id}/professor")
    public String vincularProfessor(@PathVariable Long id, @RequestParam(required = false) Long professorId,
                                    RedirectAttributes ra) {
        cadastro.vincularProfessor(id, professorId);
        ra.addFlashAttribute("sucesso", "Professor responsável atualizado.");
        return "redirect:/admin/turmas/" + id;
    }

    /** Pedido do cliente (validação do RF04): remover aluno vinculado por engano. */
    @PostMapping("/turmas/{id}/alunos/{alunoId}/remover")
    public String removerAluno(@PathVariable Long id, @PathVariable Long alunoId, RedirectAttributes ra) {
        cadastro.removerAluno(id, alunoId);
        ra.addFlashAttribute("sucesso", "Aluno removido da turma.");
        return "redirect:/admin/turmas/" + id;
    }

    /** Pedido do cliente (validação do RF04): mais de um professor por turma. */
    @PostMapping("/turmas/{id}/professores")
    public String adicionarProfessor(@PathVariable Long id, @RequestParam(required = false) Long professorId,
                                     RedirectAttributes ra) {
        cadastro.adicionarProfessor(id, professorId);
        ra.addFlashAttribute("sucesso", "Professor adicionado à turma.");
        return "redirect:/admin/turmas/" + id;
    }

    @PostMapping("/turmas/{id}/professores/{professorId}/remover")
    public String removerProfessor(@PathVariable Long id, @PathVariable Long professorId, RedirectAttributes ra) {
        cadastro.removerProfessor(id, professorId);
        ra.addFlashAttribute("sucesso", "Professor removido da turma.");
        return "redirect:/admin/turmas/" + id;
    }

    /** Pedido do cliente (validação do RF04): sala da turma. */
    @PostMapping("/turmas/{id}/sala")
    public String atualizarSala(@PathVariable Long id, @RequestParam(required = false) String sala,
                                RedirectAttributes ra) {
        cadastro.atualizarSala(id, sala);
        ra.addFlashAttribute("sucesso", "Sala atualizada.");
        return "redirect:/admin/turmas/" + id;
    }
    // O encerramento da turma passou para o professor (pedido do cliente): ver ProfessorController.
}
