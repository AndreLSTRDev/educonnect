package br.ucsal.educonnect.repository;

import br.ucsal.educonnect.model.RegistroFrequencia;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FrequenciaRepository extends JpaRepository<RegistroFrequencia, Long> {

    Optional<RegistroFrequencia> findByTurmaIdAndAlunoIdAndDataRegistro(Long turmaId, Long alunoId, LocalDate data);

    List<RegistroFrequencia> findByTurmaIdAndDataRegistro(Long turmaId, LocalDate data);

    long countByTurmaIdAndAlunoId(Long turmaId, Long alunoId);

    long countByTurmaIdAndAlunoIdAndPresenteTrue(Long turmaId, Long alunoId);

    long countByTurmaIdAndAlunoIdAndPresenteFalse(Long turmaId, Long alunoId);

    long countByTurmaIdAndAlunoIdAndPresenteFalseAndJustificativaIsNotNull(Long turmaId, Long alunoId);

}
