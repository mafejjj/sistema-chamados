package br.com.example.chamados.repository;

import java.time.DayOfWeek;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import br.com.example.chamados.domain.GradeHorario;
import br.com.example.chamados.domain.Materia;

public interface GradeHorarioRepository extends JpaRepository<GradeHorario, Long> {

	List<GradeHorario> findByDiaSemana(DayOfWeek diaSemana);

	List<GradeHorario> findByMateriaAndDiaSemana(Materia materia, DayOfWeek diaSemana);

	void deleteByMateria(Materia materia);
}
