package br.com.example.chamados.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import br.com.example.chamados.domain.Materia;

public interface MateriaRepository extends JpaRepository<Materia, Long> {

	List<Materia> findAllByOrderBySemestreAscNomeAsc();

	Optional<Materia> findFirstByOrderBySemestreDesc();
}
