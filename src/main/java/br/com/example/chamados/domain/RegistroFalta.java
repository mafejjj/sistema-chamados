package br.com.example.chamados.domain;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/** Falta confirmada em uma matéria, em uma data. {@code quantidade} é o número de aulas perdidas. */
@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = { "materia_id", "data_falta" }))
public class RegistroFalta {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@ManyToOne(optional = false)
	@JoinColumn(name = "materia_id")
	private Materia materia;

	@Column(name = "data_falta", nullable = false)
	private LocalDate data;

	@Column(nullable = false)
	private int quantidade;

	protected RegistroFalta() {
	}

	public RegistroFalta(Materia materia, LocalDate data, int quantidade) {
		this.materia = materia;
		this.data = data;
		this.quantidade = quantidade;
	}

	public Long getId() {
		return id;
	}

	public Materia getMateria() {
		return materia;
	}

	public LocalDate getData() {
		return data;
	}

	public int getQuantidade() {
		return quantidade;
	}
}
