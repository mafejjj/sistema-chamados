package br.com.example.chamados.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import br.com.example.chamados.domain.Materia;
import br.com.example.chamados.domain.RegistroFalta;

public interface RegistroFaltaRepository extends JpaRepository<RegistroFalta, Long> {

	Optional<RegistroFalta> findByMateriaAndData(Materia materia, LocalDate data);

	List<RegistroFalta> findByData(LocalDate data);

	List<RegistroFalta> findAllByOrderByDataDesc();

	List<RegistroFalta> findByMateriaOrderByDataAsc(Materia materia);

	List<RegistroFalta> findByMateriaAndDataBetweenOrderByDataAsc(Materia materia, LocalDate inicio, LocalDate fim);

	void deleteByMateria(Materia materia);

	@Query("select coalesce(sum(r.quantidade), 0) from RegistroFalta r where r.materia = :materia")
	long total(@Param("materia") Materia materia);

	@Query("""
			select coalesce(sum(r.quantidade), 0) from RegistroFalta r
			where r.materia = :materia and r.data between :inicio and :fim
			""")
	long totalNoPeriodo(@Param("materia") Materia materia, @Param("inicio") LocalDate inicio,
			@Param("fim") LocalDate fim);
}
