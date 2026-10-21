package br.com.example.chamados.repository;

import java.time.LocalDate;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import br.com.example.chamados.domain.Periodo;

public interface PeriodoRepository extends JpaRepository<Periodo, Long> {

	Optional<Periodo> findBySemestreAndBimestre(int semestre, Integer bimestre);

	Optional<Periodo> findBySemestreAndBimestreIsNull(int semestre);

	Optional<Periodo> findFirstByBimestreIsNullAndInicioLessThanEqualAndFimGreaterThanEqual(LocalDate a, LocalDate b);

	Optional<Periodo> findFirstByBimestreIsNotNullAndInicioLessThanEqualAndFimGreaterThanEqual(LocalDate a,
			LocalDate b);
}
