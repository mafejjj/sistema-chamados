package br.com.example.chamados.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
public class Materia {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, unique = true)
	private String nome;

	@Column(nullable = false)
	private int semestre;

	@Column(nullable = false)
	private int limiteFaltas;

	@Column(nullable = false)
	private int aulasPorEncontro;

	protected Materia() {
	}

	public Materia(String nome, int semestre, int limiteFaltas, int aulasPorEncontro) {
		this.nome = nome;
		this.semestre = semestre;
		this.limiteFaltas = limiteFaltas;
		this.aulasPorEncontro = aulasPorEncontro;
	}

	public Long getId() {
		return id;
	}

	public String getNome() {
		return nome;
	}

	public int getSemestre() {
		return semestre;
	}

	public int getLimiteFaltas() {
		return limiteFaltas;
	}

	public int getAulasPorEncontro() {
		return aulasPorEncontro;
	}
}
