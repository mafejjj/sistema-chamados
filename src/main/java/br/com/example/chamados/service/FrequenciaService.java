package br.com.example.chamados.service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.example.chamados.domain.Feriado;
import br.com.example.chamados.domain.GradeHorario;
import br.com.example.chamados.domain.Materia;
import br.com.example.chamados.domain.Periodo;
import br.com.example.chamados.domain.RegistroFalta;
import br.com.example.chamados.repository.FeriadoRepository;
import br.com.example.chamados.repository.GradeHorarioRepository;
import br.com.example.chamados.repository.MateriaRepository;
import br.com.example.chamados.repository.RegistroFaltaRepository;

/** Regras de frequência: o que acontece em cada dia, registro de faltas, totais e simulações. */
@Service
@Transactional
public class FrequenciaService {

	private static final Locale PT_BR = Locale.of("pt", "BR");

	private final MateriaRepository materias;
	private final GradeHorarioRepository grade;
	private final FeriadoRepository feriados;
	private final RegistroFaltaRepository registros;

	public FrequenciaService(MateriaRepository materias, GradeHorarioRepository grade, FeriadoRepository feriados,
			RegistroFaltaRepository registros) {
		this.materias = materias;
		this.grade = grade;
		this.feriados = feriados;
		this.registros = registros;
	}

	/** Linha da simulação: situação atual de uma matéria e o que as ausências acrescentariam. */
	public record LinhaSimulacao(Materia materia, long atual, int adicional, List<LocalDate> datas) {
		public long depois() {
			return atual + adicional;
		}
	}

	public record DiaSemAula(LocalDate data, String motivo) {
	}

	public record Simulacao(List<LinhaSimulacao> linhas, List<DiaSemAula> diasSemAula) {
	}

	@Transactional(readOnly = true)
	public Optional<Feriado> feriado(LocalDate data) {
		return feriados.findByData(data);
	}

	/** Aulas previstas na grade para o dia. Vazio se o dia for feriado. */
	@Transactional(readOnly = true)
	public List<GradeHorario> aulasDoDia(LocalDate data) {
		if (feriados.findByData(data).isPresent()) {
			return List.of();
		}
		return grade.findByDiaSemana(data.getDayOfWeek());
	}

	/** Registra a falta com o número de aulas que a matéria tem naquele dia da semana. */
	public RegistroFalta registrar(Materia materia, LocalDate data) {
		Optional<Feriado> feriado = feriados.findByData(data);
		if (feriado.isPresent()) {
			throw new RegraNegocioException(
					"Dia %s é feriado/sem aula (%s).".formatted(formatar(data), feriado.get().getDescricao()));
		}
		int aulas = grade.findByMateriaAndDiaSemana(materia, data.getDayOfWeek()).stream()
				.mapToInt(GradeHorario::getAulas).sum();
		if (aulas == 0) {
			throw new RegraNegocioException("Não há aula de %s na grade em %s.".formatted(materia.getNome(),
					nomeDia(data.getDayOfWeek())));
		}
		if (registros.findByMateriaAndData(materia, data).isPresent()) {
			throw new RegraNegocioException(
					"Já existe falta registrada em %s em %s.".formatted(materia.getNome(), formatar(data)));
		}
		return registros.save(new RegistroFalta(materia, data, aulas));
	}

	public void remover(Long registroId) {
		registros.deleteById(registroId);
	}

	/** Remove a matéria junto com sua grade e suas faltas. */
	public void removerMateria(Long materiaId) {
		Materia materia = materias.findById(materiaId)
				.orElseThrow(() -> new RegraNegocioException("Matéria não encontrada."));
		registros.deleteByMateria(materia);
		grade.deleteByMateria(materia);
		materias.delete(materia);
	}

	@Transactional(readOnly = true)
	public long total(Materia materia) {
		return registros.total(materia);
	}

	@Transactional(readOnly = true)
	public long total(Materia materia, Periodo periodo) {
		return registros.totalNoPeriodo(materia, periodo.getInicio(), periodo.getFim());
	}

	/** Calcula como ficariam as faltas se o aluno faltasse em todas as aulas dos dias informados. Não grava nada. */
	@Transactional(readOnly = true)
	public Simulacao simular(List<LocalDate> datas) {
		Map<Long, Acumulador> porMateria = new LinkedHashMap<>();
		List<DiaSemAula> semAula = new ArrayList<>();

		for (LocalDate data : datas.stream().distinct().sorted().toList()) {
			Optional<Feriado> feriado = feriados.findByData(data);
			if (feriado.isPresent()) {
				semAula.add(new DiaSemAula(data, "feriado: " + feriado.get().getDescricao()));
				continue;
			}
			List<GradeHorario> aulas = grade.findByDiaSemana(data.getDayOfWeek());
			if (aulas.isEmpty()) {
				semAula.add(new DiaSemAula(data, "sem aulas na grade"));
				continue;
			}
			for (GradeHorario g : aulas) {
				if (registros.findByMateriaAndData(g.getMateria(), data).isPresent()) {
					continue; // já contabilizada
				}
				Acumulador acc = porMateria.computeIfAbsent(g.getMateria().getId(), id -> new Acumulador(g.getMateria()));
				acc.adicional += g.getAulas();
				if (!acc.datas.contains(data)) {
					acc.datas.add(data);
				}
			}
		}

		List<LinhaSimulacao> linhas = porMateria.values().stream()
				.map(a -> new LinhaSimulacao(a.materia, registros.total(a.materia), a.adicional, a.datas)).toList();
		return new Simulacao(linhas, semAula);
	}

	public static String nomeDia(DayOfWeek dia) {
		return dia.getDisplayName(TextStyle.FULL, PT_BR);
	}

	private static String formatar(LocalDate data) {
		return "%02d/%02d".formatted(data.getDayOfMonth(), data.getMonthValue());
	}

	private static final class Acumulador {
		final Materia materia;
		int adicional;
		final List<LocalDate> datas = new ArrayList<>();

		Acumulador(Materia materia) {
			this.materia = materia;
		}
	}
}
