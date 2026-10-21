package br.com.example.chamados.repository;

import java.time.LocalDate;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import br.com.example.chamados.domain.Feriado;

public interface FeriadoRepository extends JpaRepository<Feriado, Long> {

	Optional<Feriado> findByData(LocalDate data);
}
