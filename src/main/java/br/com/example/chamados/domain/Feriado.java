package br.com.example.chamados.domain;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

/** Dia sem aula (feriado, recesso etc.). */
@Entity
public class Feriado {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "data_feriado", nullable = false, unique = true)
	private LocalDate data;

	@Column(nullable = false)
	private String descricao;

	protected Feriado() {
	}

	public Feriado(LocalDate data, String descricao) {
		this.data = data;
		this.descricao = descricao;
	}

	public Long getId() {
		return id;
	}

	public LocalDate getData() {
		return data;
	}

	public String getDescricao() {
		return descricao;
	}
}
