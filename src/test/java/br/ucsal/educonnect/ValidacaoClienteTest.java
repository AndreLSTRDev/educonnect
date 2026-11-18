package br.ucsal.educonnect;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.ucsal.educonnect.exception.RegraNegocioException;
import br.ucsal.educonnect.model.CategoriaDenuncia;
import br.ucsal.educonnect.model.Denuncia;
import br.ucsal.educonnect.model.Perfil;
import br.ucsal.educonnect.model.SituacaoDisciplina;
import br.ucsal.educonnect.model.StatusDenuncia;
import br.ucsal.educonnect.security.UsuarioDetailsService;
import br.ucsal.educonnect.service.AvaliacaoService;
import br.ucsal.educonnect.service.CadastroService;
import br.ucsal.educonnect.service.DenunciaService;
import br.ucsal.educonnect.service.DenunciaService.ArquivoEnviado;
import br.ucsal.educonnect.service.DenunciaService.FiltroDenuncia;
import br.ucsal.educonnect.service.DesempenhoService;
import br.ucsal.educonnect.service.FrequenciaService;
import br.ucsal.educonnect.service.SenhaService;
import br.ucsal.educonnect.service.UsuarioService;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

/** Testes dos ajustes pedidos pelo cliente no formulário de validação das telas. */
@SpringBootTest
@Transactional
class ValidacaoClienteTest {

    @Autowired UsuarioService usuarios;
    @Autowired UsuarioDetailsService detalhes;
    @Autowired SenhaService senhas;
    @Autowired CadastroService cadastro;
    @Autowired AvaliacaoService avaliacoes;
    @Autowired FrequenciaService frequencia;
    @Autowired DesempenhoService desempenho;
    @Autowired DenunciaService denuncias;
    @Autowired PasswordEncoder encoder;

    @Test
    void rf01_entraComEmailERedefineSenhaPorLinkDeUsoUnico() {
        var u = usuarios.cadastrar(Perfil.ALUNO, "Bia", "bia1", "bia@ucsal.br", "senha1234", null);
        assertThat(detalhes.loadUserByUsername("BIA@ucsal.br").getUsername()).isEqualTo("bia1");
        assertThatThrownBy(() -> usuarios.cadastrar(Perfil.ALUNO, "Outra", "bia2", "bia@ucsal.br", "senha1234", null))
                .isInstanceOf(RegraNegocioException.class).hasMessageContaining("e-mail");

        String token = senhas.solicitar("bia@ucsal.br", "http://localhost").orElseThrow();
        assertThat(senhas.solicitar("ninguem", "http://localhost")).isEmpty();
        assertThatThrownBy(() -> senhas.redefinir(token, "novaSenha1", "diferente"))
                .isInstanceOf(RegraNegocioException.class);
        senhas.redefinir(token, "novaSenha1", "novaSenha1");
        assertThat(encoder.matches("novaSenha1", usuarios.buscar(u.getId()).getSenhaHash())).isTrue();
        assertThat(senhas.tokenValido(token)).isFalse();
    }

    @Test
    void rf02_buscaEFiltroDeUsuarios() {
        usuarios.cadastrar(Perfil.ALUNO, "Carlos Lima", "c1", null, "senha1234", null);
        usuarios.cadastrar(Perfil.PROFESSOR, "Carla Souza", "c2", null, "senha1234", null);
        assertThat(usuarios.listar("carl", null)).hasSize(2);
        assertThat(usuarios.listar("carl", Perfil.PROFESSOR)).extracting("identificadorInstitucional").containsExactly("c2");
    }

    @Test
    void rf03_ementaPreRequisitosEdicaoEExclusao() {
        var curso = cadastro.criarCurso("Sistemas");
        var base = cadastro.criarDisciplina(curso.getId(), "Algoritmos", "SI1", "60");
        var avancada = cadastro.criarDisciplina(curso.getId(), "Estruturas", "SI2", "60", "Listas e árvores", List.of(base.getId()));
        assertThat(avancada.getPreRequisitos()).containsExactly(base);
        assertThatThrownBy(() -> cadastro.excluirDisciplina(base.getId()))
                .isInstanceOf(RegraNegocioException.class).hasMessageContaining("pré-requisito");
        cadastro.atualizarDisciplina(avancada.getId(), curso.getId(), "Estruturas de Dados", "SI2", "80", null, List.of());
        assertThat(cadastro.buscarDisciplina(avancada.getId()).getCargaHoraria()).isEqualTo(80);
        cadastro.excluirDisciplina(base.getId());
        assertThatThrownBy(() -> cadastro.excluirCurso(curso.getId())).isInstanceOf(RegraNegocioException.class);
        cadastro.renomearCurso(curso.getId(), "Sistemas de Informação");
        assertThat(cadastro.buscarCurso(curso.getId()).getNome()).isEqualTo("Sistemas de Informação");
    }

    @Test
    void rf04_salaVariosProfessoresRemoverAlunoEEncerramentoPeloProfessor() {
        var prof = usuarios.cadastrar(Perfil.PROFESSOR, "Prof A", "pa", null, "senha1234", null);
        var prof2 = usuarios.cadastrar(Perfil.PROFESSOR, "Prof B", "pb", null, "senha1234", null);
        var outro = usuarios.cadastrar(Perfil.PROFESSOR, "Prof C", "pc", null, "senha1234", null);
        var aluno = usuarios.cadastrar(Perfil.ALUNO, "Aluno", "al", null, "senha1234", null);
        var curso = cadastro.criarCurso("Direito");
        var disc = cadastro.criarDisciplina(curso.getId(), "Civil", "DIR1", "60");
        var turma = cadastro.criarTurma(disc.getId(), prof.getId(), "T1", "2026.1", "A-101");
        assertThat(turma.getSala()).isEqualTo("A-101");

        cadastro.adicionarProfessor(turma.getId(), prof2.getId());
        assertThat(cadastro.turmasDoProfessor(prof2.getId())).extracting("id").containsExactly(turma.getId());
        assertThatThrownBy(() -> cadastro.turmaDoProfessor(outro.getId(), turma.getId()))
                .isInstanceOf(AccessDeniedException.class);

        cadastro.vincularAluno(turma.getId(), aluno.getId());
        cadastro.removerAluno(turma.getId(), aluno.getId());
        assertThat(cadastro.buscarTurma(turma.getId()).getAlunos()).isEmpty();

        cadastro.vincularAluno(turma.getId(), aluno.getId());
        var prova = avaliacoes.cadastrarAvaliacao(prof2.getId(), turma.getId(), "P1", LocalDate.now().toString(), "10");
        avaliacoes.lancarNota(prof2.getId(), prova.getId(), aluno.getId(), 5.0);
        assertThatThrownBy(() -> cadastro.removerAluno(turma.getId(), aluno.getId()))
                .isInstanceOf(RegraNegocioException.class).hasMessageContaining("notas");

        assertThatThrownBy(() -> cadastro.encerrarTurma(outro.getId(), turma.getId()))
                .isInstanceOf(AccessDeniedException.class);
        cadastro.encerrarTurma(prof2.getId(), turma.getId());
        assertThat(cadastro.buscarTurma(turma.getId()).isEncerrada()).isTrue();
    }

    @Test
    void rf06_rf07_justificativaFaltasESituacaoParcialEmRisco() {
        var prof = usuarios.cadastrar(Perfil.PROFESSOR, "Prof", "pf", null, "senha1234", null);
        var aluno = usuarios.cadastrar(Perfil.ALUNO, "Aluno", "an", null, "senha1234", null);
        var curso = cadastro.criarCurso("Medicina");
        var disc = cadastro.criarDisciplina(curso.getId(), "Anatomia", "MED1", "60");
        var turma = cadastro.criarTurma(disc.getId(), prof.getId(), "T1", "2026.1");
        cadastro.vincularAluno(turma.getId(), aluno.getId());

        frequencia.registrar(prof.getId(), turma.getId(), LocalDate.now(), List.of(), Map.of(aluno.getId(), "Atestado"));
        assertThat(frequencia.faltas(turma.getId(), aluno.getId())).isEqualTo(1);
        assertThat(frequencia.faltasJustificadas(turma.getId(), aluno.getId())).isEqualTo(1);

        frequencia.registrar(prof.getId(), turma.getId(), LocalDate.now().minusDays(1), List.of(aluno.getId()));
        frequencia.registrar(prof.getId(), turma.getId(), LocalDate.now().minusDays(2), List.of(aluno.getId()));
        frequencia.registrar(prof.getId(), turma.getId(), LocalDate.now().minusDays(3), List.of(aluno.getId()));
        var prova = avaliacoes.cadastrarAvaliacao(prof.getId(), turma.getId(), "P1", LocalDate.now().toString(), "10");
        avaliacoes.lancarNota(prof.getId(), prova.getId(), aluno.getId(), 4.0);

        var linha = desempenho.desempenho(aluno.getId(), "2026.1").get(0);
        assertThat(linha.faltas()).isEqualTo(1);
        assertThat(linha.situacao()).isEqualTo(SituacaoDisciplina.EM_RISCO_POR_NOTA);
        assertThat(desempenho.evolucao(aluno.getId(), "2026.1")).hasSize(1)
                .first().satisfies(s -> assertThat(s.pontos()).hasSize(1));
    }

    @Test
    void rf09_rf10_anonimaAnexosFiltrosEComentarioObrigatorio() {
        var aluno = usuarios.cadastrar(Perfil.ALUNO, "Duda Ramos", "du", "du@ucsal.br", "senha1234", null);
        var gestor = usuarios.cadastrar(Perfil.GESTOR_DENUNCIAS, "Gestor", "gs", null, "senha1234", null);
        var pdf = new ArquivoEnviado("prova.pdf", "application/pdf", new byte[]{1, 2, 3});

        Denuncia anonima = denuncias.registrar(aluno.getId(), "Assédio no corredor", CategoriaDenuncia.ASSEDIO,
                "Relato", true, List.of(pdf));
        Denuncia aberta = denuncias.registrar(aluno.getId(), "Lâmpada", CategoriaDenuncia.INFRAESTRUTURA, "Queimada");
        assertThat(anonima.getAutorExibido()).isEqualTo("Anônimo");
        assertThat(anonima.getAnexos()).hasSize(1);
        assertThatThrownBy(() -> denuncias.registrar(aluno.getId(), "X", CategoriaDenuncia.OUTRA, "Y", false,
                List.of(new ArquivoEnviado("virus.exe", "application/octet-stream", new byte[]{1}))))
                .isInstanceOf(RegraNegocioException.class).hasMessageContaining("não é permitido");

        assertThat(denuncias.listar(new FiltroDenuncia(null, CategoriaDenuncia.ASSEDIO, null, null, null, null)))
                .containsExactly(anonima);
        // Busca por nome não revela a autoria das denúncias anônimas.
        assertThat(denuncias.listar(new FiltroDenuncia(null, null, null, null, "duda", null))).containsExactly(aberta);
        assertThat(denuncias.listar(new FiltroDenuncia(null, null, null, null, null, aberta.getProtocolo())))
                .containsExactly(aberta);
        assertThat(denuncias.listar(new FiltroDenuncia(null, null, LocalDate.now().plusDays(1), null, null, null))).isEmpty();

        assertThatThrownBy(() -> denuncias.atualizarStatus(gestor.getId(), aberta.getId(), StatusDenuncia.EM_ANALISE, " "))
                .isInstanceOf(RegraNegocioException.class).hasMessageContaining("Comentário");
        denuncias.atualizarStatus(gestor.getId(), aberta.getId(), StatusDenuncia.EM_ANALISE, "Chamado aberto");
        assertThat(aberta.getStatus()).isEqualTo(StatusDenuncia.EM_ANALISE);
    }
}
