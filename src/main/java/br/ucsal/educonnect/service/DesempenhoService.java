package br.ucsal.educonnect.service;

import br.ucsal.educonnect.model.Nota;
import br.ucsal.educonnect.model.SituacaoDisciplina;
import br.ucsal.educonnect.model.Turma;
import br.ucsal.educonnect.repository.NotaRepository;
import br.ucsal.educonnect.repository.TurmaRepository;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** RF07 (desempenho) e RF08 (histórico acadêmico). */
@Service
public class DesempenhoService {

    private final TurmaRepository turmas;
    private final NotaRepository notas;
    private final FrequenciaService frequencia;
    private final double escalaMaxima;
    private final double mediaMinima;
    private final double frequenciaMinima;

    public DesempenhoService(TurmaRepository turmas, NotaRepository notas, FrequenciaService frequencia,
                             @Value("${educonnect.escala-maxima:10.0}") double escalaMaxima,
                             @Value("${educonnect.media-minima:6.0}") double mediaMinima,
                             @Value("${educonnect.frequencia-minima:75.0}") double frequenciaMinima) {
        this.turmas = turmas;
        this.notas = notas;
        this.frequencia = frequencia;
        this.escalaMaxima = escalaMaxima;
        this.mediaMinima = mediaMinima;
        this.frequenciaMinima = frequenciaMinima;
    }

    /** Períodos em que o aluno possui turmas, do mais recente para o mais antigo. */
    @Transactional(readOnly = true)
    public List<String> periodos(Long alunoId) {
        return turmas.findByAlunoId(alunoId).stream()
                .map(Turma::getPeriodo).distinct()
                .sorted(Comparator.reverseOrder()).toList();
    }

    /** RF07: 100% das disciplinas do período, com média, frequência e situação. */
    @Transactional(readOnly = true)
    public List<LinhaDesempenho> desempenho(Long alunoId, String periodo) {
        return turmas.findByAlunoId(alunoId).stream()
                .filter(t -> periodo == null || periodo.isBlank() || t.getPeriodo().equals(periodo))
                .sorted(Comparator.comparing(Turma::getPeriodo).reversed()
                        .thenComparing(t -> t.getDisciplina().getNome()))
                .map(t -> linha(alunoId, t))
                .toList();
    }

    /** RF08: todas as disciplinas cursadas, com notas e resultados. */
    @Transactional(readOnly = true)
    public List<LinhaDesempenho> historico(Long alunoId) {
        return desempenho(alunoId, null);
    }

    @Transactional(readOnly = true)
    public List<Nota> notasDaTurma(Long alunoId, Long turmaId) {
        return notas.findByAlunoETurma(alunoId, turmaId).stream()
                .sorted(Comparator.comparing(n -> n.getAvaliacao().getDataAvaliacao()))
                .toList();
    }

    /**
     * Média da turma na escala da instituição: soma das notas dividida pela soma dos valores
     * máximos das avaliações já corrigidas, multiplicada pela escala. Nulo se não há notas.
     */
    @Transactional(readOnly = true)
    public Double calcularMedia(Long alunoId, Long turmaId) {
        List<Nota> lista = notas.findByAlunoETurma(alunoId, turmaId);
        double obtido = lista.stream().mapToDouble(Nota::getValor).sum();
        double maximo = lista.stream().mapToDouble(n -> n.getAvaliacao().getValorMaximo()).sum();
        if (maximo == 0) {
            return null;
        }
        return arredondar(obtido / maximo * escalaMaxima);
    }

    /** Sobrecarga: média geral do aluno em um período (média das médias das disciplinas). */
    @Transactional(readOnly = true)
    public Double calcularMedia(Long alunoId, String periodo) {
        List<Double> medias = turmas.findByAlunoId(alunoId).stream()
                .filter(t -> t.getPeriodo().equals(periodo))
                .map(t -> calcularMedia(alunoId, t.getId()))
                .filter(Objects::nonNull).toList();
        if (medias.isEmpty()) {
            return null;
        }
        return arredondar(medias.stream().mapToDouble(Double::doubleValue).average().orElse(0));
    }

    SituacaoDisciplina situacao(boolean turmaEncerrada, Double media, Double freq) {
        if (!turmaEncerrada) {
            return SituacaoDisciplina.EM_ANDAMENTO;
        }
        if (freq != null && freq < frequenciaMinima) {
            return SituacaoDisciplina.REPROVADO_POR_FALTA;
        }
        if (media == null || media < mediaMinima) {
            return SituacaoDisciplina.REPROVADO_POR_NOTA;
        }
        return SituacaoDisciplina.APROVADO;
    }

    private LinhaDesempenho linha(Long alunoId, Turma t) {
        Double media = calcularMedia(alunoId, t.getId());
        Double freq = frequencia.percentual(t.getId(), alunoId);
        Double freqArredondada = freq == null ? null : arredondar(freq);
        return new LinhaDesempenho(t.getId(), t.getDisciplina().getCodigo(), t.getDisciplina().getNome(),
                t.getDisciplina().getCargaHoraria(), t.getPeriodo(), media, freqArredondada,
                situacao(t.isEncerrada(), media, freq));
    }

    private static double arredondar(double v) {
        return Math.round(v * 100.0) / 100.0;
    }
}
