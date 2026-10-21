package br.com.example.chamados.domain;

import java.time.DayOfWeek;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

/** Uma matéria que acontece em determinado dia da semana, com N aulas. */
@Entity
public class GradeHorario {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(optional = false)
	@JoinColumn(name = "materia_id")
	private Materia materia;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private DayOfWeek diaSemana;

	@Column(nullable = false)
	private int aulas;

	protected GradeHorario() {
	}

	public GradeHorario(Materia materia, DayOfWeek diaSemana, int aulas) {
		this.materia = materia;
		this.diaSemana = diaSemana;
		this.aulas = aulas;
	}

	public Long getId() {
		return id;
	}

	public Materia getMateria() {
		return materia;
	}

	public DayOfWeek getDiaSemana() {
		return diaSemana;
	}

	public int getAulas() {
		return aulas;
	}
}
