package br.ucsal.educonnect;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.ucsal.educonnect.exception.RegraNegocioException;
import br.ucsal.educonnect.model.CategoriaDenuncia;
import br.ucsal.educonnect.model.Denuncia;
import br.ucsal.educonnect.model.Perfil;
import br.ucsal.educonnect.model.SituacaoDisciplina;
import br.ucsal.educonnect.model.StatusDenuncia;
import br.ucsal.educonnect.model.Usuario;
import br.ucsal.educonnect.service.AvaliacaoService;
import br.ucsal.educonnect.service.CadastroService;
import br.ucsal.educonnect.service.DenunciaService;
import br.ucsal.educonnect.service.DesempenhoService;
import br.ucsal.educonnect.service.FrequenciaService;
import br.ucsal.educonnect.service.UsuarioService;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class EduConnectFluxoTest {

    @Autowired UsuarioService usuarios;
    @Autowired CadastroService cadastro;
    @Autowired AvaliacaoService avaliacoes;
    @Autowired FrequenciaService frequencia;
    @Autowired DesempenhoService desempenho;
    @Autowired DenunciaService denuncias;
    @Autowired PasswordEncoder encoder;

    @Test
    void rf02_impedeIdentificadorDuplicadoESenhaNaoFicaEmTextoPuro() {
        Usuario u = usuarios.cadastrar(Perfil.ALUNO, "Ana", "ana1", null, "senha1234", null);
        assertThat(u.getSenhaHash()).isNotEqualTo("senha1234");
        assertThat(u.autenticar("senha1234", encoder)).isTrue();
        assertThat(u.autenticar("errada", encoder)).isFalse();
        assertThatThrownBy(() -> usuarios.cadastrar(Perfil.PROFESSOR, "Outra", "ANA1", null, "senha1234", null))
                .isInstanceOf(RegraNegocioException.class).hasMessageContaining("já está em uso");
    }

    @Test
    void rf04_rf05_rf06_rf07_fluxoAcademicoCompleto() {
        var prof = usuarios.cadastrar(Perfil.PROFESSOR, "Prof", "p1", null, "senha1234", null);
        var aluno = usuarios.cadastrar(Perfil.ALUNO, "Aluno", "a1", null, "senha1234", null);
        var curso = cadastro.criarCurso("ES");
        var disc = cadastro.criarDisciplina(curso.getId(), "Requisitos", "ES1", "60");
        var turma = cadastro.criarTurma(disc.getId(), prof.getId(), "T1", "2026.1");

        cadastro.vincularAluno(turma.getId(), aluno.getId());
        assertThatThrownBy(() -> cadastro.vincularAluno(turma.getId(), aluno.getId()))
                .isInstanceOf(RegraNegocioException.class).hasMessageContaining("já está vinculado");

        var prova = avaliacoes.cadastrarAvaliacao(prof.getId(), turma.getId(), "Prova", LocalDate.now().toString(), "10");
        assertThatThrownBy(() -> avaliacoes.lancarNota(prof.getId(), prova.getId(), aluno.getId(), 11.0))
                .isInstanceOf(RegraNegocioException.class).hasMessageContaining("fora da escala");
        avaliacoes.lancarNota(prof.getId(), prova.getId(), aluno.getId(), 8.0);

        frequencia.registrar(prof.getId(), turma.getId(), LocalDate.now().minusDays(1), List.of(aluno.getId()));
        frequencia.registrar(prof.getId(), turma.getId(), LocalDate.now(), List.of());
        assertThat(frequencia.percentual(turma.getId(), aluno.getId())).isEqualTo(50.0);

        assertThat(desempenho.calcularMedia(aluno.getId(), turma.getId())).isEqualTo(8.0);
        assertThat(desempenho.calcularMedia(aluno.getId(), "2026.1")).isEqualTo(8.0);
        assertThat(desempenho.desempenho(aluno.getId(), "2026.1")).hasSize(1)
                .first().satisfies(l -> assertThat(l.situacao()).isEqualTo(SituacaoDisciplina.EM_RISCO_POR_FALTA));

        cadastro.encerrarTurma(prof.getId(), turma.getId());
        assertThat(desempenho.historico(aluno.getId()).get(0).situacao())
                .isEqualTo(SituacaoDisciplina.REPROVADO_POR_FALTA);
    }

    @Test
    void rf09_rf10_geraProtocoloERegistraHistoricoDeStatus() {
        var aluno = usuarios.cadastrar(Perfil.ALUNO, "Aluno", "a2", null, "senha1234", null);
        var gestor = usuarios.cadastrar(Perfil.GESTOR_DENUNCIAS, "Gestor", "g1", null, "senha1234", null);

        Denuncia d = denuncias.registrar(aluno.getId(), "Ruído", CategoriaDenuncia.INFRAESTRUTURA, "Sala 3 sem isolamento");
        assertThat(d.getProtocolo()).matches("EDU-\\d{4}-\\d{6}");
        assertThat(d.getStatus()).isEqualTo(StatusDenuncia.ABERTA);

        denuncias.atualizarStatus(gestor.getId(), d.getId(), StatusDenuncia.EM_ANALISE, "Em análise");
        assertThat(d.getStatus()).isEqualTo(StatusDenuncia.EM_ANALISE);
        assertThat(d.getHistorico()).hasSize(2);
        assertThatThrownBy(() -> denuncias.registrar(aluno.getId(), " ", CategoriaDenuncia.OUTRA, "x"))
                .isInstanceOf(RegraNegocioException.class);
    }
}
