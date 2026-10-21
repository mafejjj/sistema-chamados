package br.com.example.chamados.web;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import br.com.example.chamados.domain.Feriado;
import br.com.example.chamados.domain.GradeHorario;
import br.com.example.chamados.domain.Materia;
import br.com.example.chamados.domain.Periodo;
import br.com.example.chamados.domain.RegistroFalta;
import br.com.example.chamados.repository.FeriadoRepository;
import br.com.example.chamados.repository.GradeHorarioRepository;
import br.com.example.chamados.repository.MateriaRepository;
import br.com.example.chamados.repository.PeriodoRepository;
import br.com.example.chamados.repository.RegistroFaltaRepository;
import br.com.example.chamados.service.FrequenciaService;
import br.com.example.chamados.service.RegraNegocioException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** Cadastros base: matérias, grade semanal, períodos acadêmicos e feriados. */
@RestController
@RequestMapping("/api")
public class CadastroController {

	public record NovaMateria(@NotBlank String nome, @Min(1) int semestre, @Min(1) int limiteFaltas,
			@Min(1) @Max(10) int aulasPorEncontro) {
	}

	/** {@code aulas} é opcional: se omitido, usa as aulas por encontro da matéria. */
	public record NovaGrade(@NotNull Long materiaId, @NotNull DayOfWeek diaSemana, @Min(1) @Max(10) Integer aulas) {
	}

	/** {@code bimestre} nulo = semestre inteiro. */
	public record NovoPeriodo(@Min(1) int semestre, @Min(1) @Max(4) Integer bimestre, @NotNull LocalDate inicio,
			@NotNull LocalDate fim) {
	}

	public record NovoFeriado(@NotNull LocalDate data, @NotBlank String descricao) {
	}

	private final MateriaRepository materias;
	private final GradeHorarioRepository grade;
	private final PeriodoRepository periodos;
	private final FeriadoRepository feriados;
	private final RegistroFaltaRepository registros;
	private final FrequenciaService frequencia;

	public CadastroController(MateriaRepository materias, GradeHorarioRepository grade, PeriodoRepository periodos,
			FeriadoRepository feriados, RegistroFaltaRepository registros, FrequenciaService frequencia) {
		this.materias = materias;
		this.grade = grade;
		this.periodos = periodos;
		this.feriados = feriados;
		this.registros = registros;
		this.frequencia = frequencia;
	}

	// matérias
	@GetMapping("/materias")
	public List<Materia> listarMaterias() {
		return materias.findAllByOrderBySemestreAscNomeAsc();
	}

	@PostMapping("/materias")
	@ResponseStatus(HttpStatus.CREATED)
	public Materia criarMateria(@Valid @RequestBody NovaMateria m) {
		return materias.save(new Materia(m.nome().trim(), m.semestre(), m.limiteFaltas(), m.aulasPorEncontro()));
	}

	@DeleteMapping("/materias/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void removerMateria(@PathVariable Long id) {
		frequencia.removerMateria(id);
	}

	// grade
	@GetMapping("/grade")
	public List<GradeHorario> listarGrade() {
		return grade.findAll().stream().sorted(
				java.util.Comparator.comparing(GradeHorario::getDiaSemana).thenComparing(g -> g.getMateria().getNome()))
				.toList();
	}

	@PostMapping("/grade")
	@ResponseStatus(HttpStatus.CREATED)
	public GradeHorario criarGrade(@Valid @RequestBody NovaGrade g) {
		Materia materia = materias.findById(g.materiaId())
				.orElseThrow(() -> new RegraNegocioException("Matéria não encontrada."));
		int aulas = g.aulas() != null ? g.aulas() : materia.getAulasPorEncontro();
		return grade.save(new GradeHorario(materia, g.diaSemana(), aulas));
	}

	@DeleteMapping("/grade/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void removerGrade(@PathVariable Long id) {
		grade.deleteById(id);
	}

	// períodos
	@GetMapping("/periodos")
	public List<Periodo> listarPeriodos() {
		return periodos.findAll();
	}

	@PostMapping("/periodos")
	@ResponseStatus(HttpStatus.CREATED)
	public Periodo criarPeriodo(@Valid @RequestBody NovoPeriodo p) {
		if (p.fim().isBefore(p.inicio())) {
			throw new RegraNegocioException("A data final não pode ser anterior à inicial.");
		}
		boolean existe = p.bimestre() == null ? periodos.findBySemestreAndBimestreIsNull(p.semestre()).isPresent()
				: periodos.findBySemestreAndBimestre(p.semestre(), p.bimestre()).isPresent();
		if (existe) {
			throw new RegraNegocioException("Esse período já está cadastrado.");
		}
		return periodos.save(new Periodo(p.semestre(), p.bimestre(), p.inicio(), p.fim()));
	}

	@DeleteMapping("/periodos/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void removerPeriodo(@PathVariable Long id) {
		periodos.deleteById(id);
	}

	// feriados
	@GetMapping("/feriados")
	public List<Feriado> listarFeriados() {
		return feriados.findAll();
	}

	@PostMapping("/feriados")
	@ResponseStatus(HttpStatus.CREATED)
	public Feriado criarFeriado(@Valid @RequestBody NovoFeriado f) {
		if (feriados.findByData(f.data()).isPresent()) {
			throw new RegraNegocioException("Esse dia já está cadastrado como feriado.");
		}
		return feriados.save(new Feriado(f.data(), f.descricao().trim()));
	}

	@DeleteMapping("/feriados/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void removerFeriado(@PathVariable Long id) {
		feriados.deleteById(id);
	}

	// faltas (somente leitura; o registro é feito pelo chat)
	@GetMapping("/faltas")
	public List<RegistroFalta> listarFaltas() {
		return registros.findAllByOrderByDataDesc();
	}
}
