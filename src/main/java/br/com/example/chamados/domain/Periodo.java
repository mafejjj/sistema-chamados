package br.com.example.chamados.domain;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

/**
 * Período acadêmico. Com {@code bimestre} nulo representa o semestre inteiro;
 * com valor, representa aquele bimestre dentro do semestre.
 */
@Entity
public class Periodo {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false)
	private int semestre;

	private Integer bimestre;

	@Column(name = "data_inicio", nullable = false)
	private LocalDate inicio;

	@Column(name = "data_fim", nullable = false)
	private LocalDate fim;

	protected Periodo() {
	}

	public Periodo(int semestre, Integer bimestre, LocalDate inicio, LocalDate fim) {
		this.semestre = semestre;
		this.bimestre = bimestre;
		this.inicio = inicio;
		this.fim = fim;
	}

	public Long getId() {
		return id;
	}

	public int getSemestre() {
		return semestre;
	}

	public Integer getBimestre() {
		return bimestre;
	}

	public LocalDate getInicio() {
		return inicio;
	}

	public LocalDate getFim() {
		return fim;
	}
}
