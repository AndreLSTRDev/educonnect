package br.ucsal.educonnect.service;

import static br.ucsal.educonnect.service.Validacao.limitar;
import static br.ucsal.educonnect.service.Validacao.numero;
import static br.ucsal.educonnect.service.Validacao.obrigatorio;
import static br.ucsal.educonnect.service.Validacao.opcional;

import br.ucsal.educonnect.exception.RecursoNaoEncontradoException;
import br.ucsal.educonnect.exception.RegraNegocioException;
import br.ucsal.educonnect.model.Aluno;
import br.ucsal.educonnect.model.Avaliacao;
import br.ucsal.educonnect.model.Nota;
import br.ucsal.educonnect.model.Turma;
import br.ucsal.educonnect.repository.AvaliacaoRepository;
import br.ucsal.educonnect.repository.NotaRepository;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** RF05 - Avaliações e notas. */
@Service
public class AvaliacaoService {

    private final AvaliacaoRepository avaliacoes;
    private final NotaRepository notas;
    private final CadastroService cadastro;
    private final double escalaMaxima;

    public AvaliacaoService(AvaliacaoRepository avaliacoes, NotaRepository notas, CadastroService cadastro,
                            @Value("${educonnect.escala-maxima:10.0}") double escalaMaxima) {
        this.avaliacoes = avaliacoes;
        this.notas = notas;
        this.cadastro = cadastro;
        this.escalaMaxima = escalaMaxima;
    }

    @Transactional(readOnly = true)
    public List<Avaliacao> listarAvaliacoes(Long turmaId) {
        return avaliacoes.findByTurmaIdOrderByDataAvaliacaoAscIdAsc(turmaId);
    }

    @Transactional(readOnly = true)
    public Avaliacao avaliacaoDoProfessor(Long professorId, Long avaliacaoId) {
        Avaliacao a = avaliacoes.findById(avaliacaoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Avaliação não encontrada."));
        if (!a.getTurma().temProfessor(professorId)) {
            throw new AccessDeniedException("Avaliação pertence a outro professor.");
        }
        return a;
    }

    @Transactional
    public Avaliacao cadastrarAvaliacao(Long professorId, Long turmaId, String nome, String data, String valorMaximo) {
        Turma turma = cadastro.turmaDoProfessor(professorId, turmaId);
        obrigatorio(nome, "Nome da avaliação");
        limitar(nome, "Nome da avaliação", 120);
        LocalDate dia;
        try {
            dia = LocalDate.parse(obrigatorio(data, "Data"));
        } catch (java.time.format.DateTimeParseException e) {
            throw new RegraNegocioException("Informe uma data válida para a avaliação.");
        }
        double max = numero(valorMaximo, "Valor máximo");
        if (max <= 0 || max > escalaMaxima) {
            throw new RegraNegocioException("O valor máximo deve ser maior que 0 e no máximo "
                    + String.format(Locale.forLanguageTag("pt-BR"), "%.1f", escalaMaxima)
                    + " (escala da instituição).");
        }
        Avaliacao a = new Avaliacao();
        a.setTurma(turma);
        a.setNome(nome.trim());
        a.setDataAvaliacao(dia);
        a.setValorMaximo(max);
        return avaliacoes.save(a);
    }

    /** Sobrecarga 1: lança a nota sem observações. */
    @Transactional
    public Nota lancarNota(Long professorId, Long avaliacaoId, Long alunoId, double valor) {
        return lancarNota(professorId, avaliacaoId, alunoId, valor, null);
    }

    /** Sobrecarga 2: lança a nota com observações (RF05, valida a escala). */
    @Transactional
    public Nota lancarNota(Long professorId, Long avaliacaoId, Long alunoId, double valor, String observacoes) {
        Avaliacao avaliacao = avaliacaoDoProfessor(professorId, avaliacaoId);
        Aluno aluno = avaliacao.getTurma().getAlunos().stream()
                .filter(a -> a.getId().equals(alunoId)).findFirst()
                .orElseThrow(() -> new RegraNegocioException("O aluno não pertence a esta turma."));
        limitar(observacoes, "Observações", 255);

        Nota nota = notas.findByAvaliacaoIdAndAlunoId(avaliacaoId, alunoId).orElseGet(Nota::new);
        nota.setAluno(aluno);
        nota.setAvaliacao(avaliacao);
        nota.setValor(valor);
        nota.setObservacoes(opcional(observacoes));
        if (!nota.validar()) {
            throw new RegraNegocioException("A nota de " + aluno.getNomeCompleto() + " (" + valor
                    + ") está fora da escala: informe um valor entre 0 e "
                    + avaliacao.getValorMaximo() + ".");
        }
        return notas.save(nota);
    }

    /** Lança todas as notas do formulário numa única transação; campos vazios são ignorados. */
    @Transactional
    public int lancarNotas(Long professorId, Long avaliacaoId, Map<Long, String> valores, Map<Long, String> observacoes) {
        int total = 0;
        for (Map.Entry<Long, String> e : valores.entrySet()) {
            if (e.getValue() == null || e.getValue().isBlank()) {
                continue;
            }
            double v = numero(e.getValue(), "Nota");
            lancarNota(professorId, avaliacaoId, e.getKey(), v, observacoes.get(e.getKey()));
            total++;
        }
        return total;
    }

    @Transactional(readOnly = true)
    public Map<Long, Nota> notasPorAluno(Long avaliacaoId) {
        Map<Long, Nota> mapa = new HashMap<>();
        notas.findByAvaliacaoId(avaliacaoId).forEach(n -> mapa.put(n.getAluno().getId(), n));
        return mapa;
    }
}
