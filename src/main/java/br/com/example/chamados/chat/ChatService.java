package br.com.example.chamados.chat;

import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import br.com.example.chamados.domain.GradeHorario;
import br.com.example.chamados.domain.Materia;
import br.com.example.chamados.domain.Periodo;
import br.com.example.chamados.domain.RegistroFalta;
import br.com.example.chamados.repository.MateriaRepository;
import br.com.example.chamados.repository.PeriodoRepository;
import br.com.example.chamados.repository.RegistroFaltaRepository;
import br.com.example.chamados.service.FrequenciaService;
import br.com.example.chamados.service.FrequenciaService.DiaSemAula;
import br.com.example.chamados.service.FrequenciaService.LinhaSimulacao;
import br.com.example.chamados.service.FrequenciaService.Simulacao;
import br.com.example.chamados.service.RegraNegocioException;

/**
 * Interpreta mensagens em português e responde em texto. Fluxo do diagrama:
 * mensagem → interpretar intenção e data → registrar / consultar / simular → responder.
 */
@Service
public class ChatService {

	static final String AJUDA = """
			Posso registrar, consultar e simular suas faltas. Exemplos:
			- Faltei hoje em Banco de Dados
			- Faltei ontem em Engenharia de Software
			- Faltei hoje em todas as matérias
			- Quantas faltas eu tenho em Banco de Dados?
			- Quantas faltas eu tenho no 2º bimestre?
			- Quantas faltas tenho em Banco de Dados no quinto semestre?
			- Vou faltar nos dias 10, 12 e 15 de maio. Como fico?
			- Remova a falta de ontem em Banco de Dados""";

	private static final DateTimeFormatter DIA_MES = DateTimeFormatter.ofPattern("dd/MM");
	private static final DateTimeFormatter DATA_COMPLETA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
	private static final Set<String> PALAVRAS_VAZIAS = Set.of("de", "da", "do", "das", "dos", "e", "em", "a", "o");
	private static final Map<String, Integer> ORDINAIS = Map.of("primeir", 1, "segund", 2, "terceir", 3, "quart", 4,
			"quint", 5, "sext", 6, "setim", 7, "oitav", 8, "non", 9, "decim", 10);
	private static final String ORDINAL = "(\\d{1,2}|primeir[oa]|segund[oa]|terceir[oa]|quart[oa]|quint[oa]|sext[oa]|setim[oa]|oitav[oa]|non[oa]|decim[oa])";

	private final MateriaRepository materias;
	private final PeriodoRepository periodos;
	private final RegistroFaltaRepository registros;
	private final FrequenciaService frequencia;
	private final DataParser datas;
	private final Clock clock;

	public ChatService(MateriaRepository materias, PeriodoRepository periodos, RegistroFaltaRepository registros,
			FrequenciaService frequencia, DataParser datas, Clock clock) {
		this.materias = materias;
		this.periodos = periodos;
		this.registros = registros;
		this.frequencia = frequencia;
		this.datas = datas;
		this.clock = clock;
	}

	public String responder(String mensagem) {
		if (mensagem == null || mensagem.isBlank()) {
			return AJUDA;
		}
		String t = Texto.normalizar(mensagem);
		try {
			if (t.matches(".*\\b(ajuda|help|comandos|o que voce faz)\\b.*")) {
				return AJUDA;
			}
			if (t.contains("falta") && t.matches(".*\\b(remov\\w*|apag\\w*|exclu\\w*|delet\\w*|cancel\\w*|desfaz\\w*)\\b.*")) {
				return remover(t);
			}
			if (t.matches(".*\\bfaltar\\b.*") || t.contains("simul")) {
				return simular(t);
			}
			if (t.matches(".*(\\bfaltei\\b|\\bnao fui\\b|\\btive falta\\b|\\bregistr\\w*\\s+(uma\\s+|a\\s+)?falta).*")) {
				return registrar(t);
			}
			if (t.matches(".*\\b(quant\\w*|resumo|situacao|minhas)\\b.*") || t.contains("falta")) {
				return consultar(t);
			}
			return "Não entendi. Digite \"ajuda\" para ver exemplos.";
		} catch (RegraNegocioException e) {
			return e.getMessage();
		}
	}

	// ---------------------------------------------------------------- registrar

	private String registrar(String t) {
		LocalDate hoje = LocalDate.now(clock);
		LocalDate data = datas.unica(t, hoje, false).orElse(hoje);
		if (data.isAfter(hoje)) {
			return "Essa data ainda não chegou. Para projetar, use: \"Vou faltar no dia %s\"."
					.formatted(data.format(DIA_MES));
		}

		List<Materia> alvo;
		if (t.matches(".*\\b(todas|todos|tudo|dia todo|dia inteiro)\\b.*")) {
			Map<Long, Materia> doDia = new LinkedHashMap<>();
			for (GradeHorario g : frequencia.aulasDoDia(data)) {
				doDia.putIfAbsent(g.getMateria().getId(), g.getMateria());
			}
			if (doDia.isEmpty()) {
				return "Não há aulas na sua grade em %s.".formatted(descrever(data));
			}
			alvo = new ArrayList<>(doDia.values());
		} else {
			List<Materia> achadas = acharMaterias(t);
			if (achadas.isEmpty()) {
				return "Em qual matéria você faltou?" + sugestoesDoDia(data);
			}
			if (achadas.size() > 1) {
				return ambiguidade(achadas);
			}
			alvo = achadas;
		}

		StringBuilder resposta = new StringBuilder();
		for (Materia materia : alvo) {
			try {
				RegistroFalta registro = frequencia.registrar(materia, data);
				long total = frequencia.total(materia);
				resposta.append("✅ Registrei %s em %s (%s).%n   Total: %s%n".formatted(
						faltas(registro.getQuantidade()), materia.getNome(), descrever(data),
						situacao(total, materia.getLimiteFaltas())));
			} catch (RegraNegocioException e) {
				resposta.append(e.getMessage()).append('\n');
			}
		}
		return resposta.toString().stripTrailing();
	}

	private String sugestoesDoDia(LocalDate data) {
		List<String> nomes = frequencia.aulasDoDia(data).stream().map(g -> g.getMateria().getNome()).distinct()
				.toList();
		if (nomes.isEmpty()) {
			return "";
		}
		return " Em %s você tem: %s.".formatted(descrever(data), String.join(", ", nomes));
	}

	// ---------------------------------------------------------------- remover

	private String remover(String t) {
		LocalDate hoje = LocalDate.now(clock);
		Optional<LocalDate> data = datas.unica(t, hoje, false);
		if (data.isEmpty()) {
			return "De qual dia? Ex.: \"Remova a falta de ontem em Banco de Dados\".";
		}
		List<RegistroFalta> candidatas = registros.findByData(data.get());
		List<Materia> achadas = acharMaterias(t);
		if (achadas.size() > 1) {
			return ambiguidade(achadas);
		}
		if (achadas.size() == 1) {
			Long id = achadas.get(0).getId();
			candidatas = candidatas.stream().filter(r -> r.getMateria().getId().equals(id)).toList();
		}
		if (candidatas.isEmpty()) {
			return "Não encontrei falta registrada em %s.".formatted(descrever(data.get()));
		}
		if (candidatas.size() > 1) {
			String nomes = candidatas.stream().map(r -> r.getMateria().getNome()).collect(Collectors.joining(", "));
			return "Há faltas em %s em mais de uma matéria (%s). Diga qual remover.".formatted(descrever(data.get()),
					nomes);
		}
		RegistroFalta registro = candidatas.get(0);
		Materia materia = registro.getMateria();
		frequencia.remover(registro.getId());
		return "🗑️ Removi %s de %s em %s.%n   Total agora: %s".formatted(faltas(registro.getQuantidade()),
				materia.getNome(), descrever(data.get()), situacao(frequencia.total(materia), materia.getLimiteFaltas()));
	}

	// ---------------------------------------------------------------- consultar

	private String consultar(String t) {
		Integer bimestre = ordinal(t, "bimestre");
		Integer semestre = ordinal(t, "semestre");

		List<Materia> todas = materias.findAllByOrderBySemestreAscNomeAsc();
		if (todas.isEmpty()) {
			return "Ainda não há matérias cadastradas.";
		}
		List<Materia> alvo = acharMaterias(t);
		if (alvo.isEmpty()) {
			alvo = todas;
		}

		Periodo periodo = null;
		String rotulo = null;
		if (bimestre != null) {
			int sem = semestre != null ? semestre : semestreAtual(todas);
			periodo = periodos.findBySemestreAndBimestre(sem, bimestre).orElse(null);
			if (periodo == null) {
				return "Não tenho as datas do %dº bimestre do %dº semestre cadastradas.".formatted(bimestre, sem);
			}
			rotulo = "%dº bimestre do %dº semestre".formatted(bimestre, sem);
		} else if (semestre != null) {
			periodo = periodos.findBySemestreAndBimestreIsNull(semestre).orElse(null);
			rotulo = "%dº semestre".formatted(semestre);
			if (periodo == null) {
				int sem = semestre;
				alvo = alvo.stream().filter(m -> m.getSemestre() == sem).toList();
				if (alvo.isEmpty()) {
					return "Não encontrei matérias do %dº semestre.".formatted(semestre);
				}
			}
		}

		StringBuilder resposta = new StringBuilder();
		if (rotulo != null) {
			resposta.append("Faltas no ").append(rotulo);
			if (periodo != null) {
				resposta.append(" (%s a %s)".formatted(periodo.getInicio().format(DIA_MES),
						periodo.getFim().format(DIA_MES)));
			}
			resposta.append(":\n");
		}
		for (Materia m : alvo) {
			if (periodo != null) {
				long n = frequencia.total(m, periodo);
				resposta.append("- %s: %s (limite do semestre: %d)%n".formatted(m.getNome(), faltas(n),
						m.getLimiteFaltas()));
			} else {
				resposta.append("- %s: %s%n".formatted(m.getNome(), situacao(frequencia.total(m), m.getLimiteFaltas())));
			}
		}
		if (alvo.size() == 1) {
			resposta.append(detalharDatas(alvo.get(0), periodo));
		}
		return resposta.toString().stripTrailing();
	}

	private String detalharDatas(Materia materia, Periodo periodo) {
		List<RegistroFalta> lista = periodo == null ? registros.findByMateriaOrderByDataAsc(materia)
				: registros.findByMateriaAndDataBetweenOrderByDataAsc(materia, periodo.getInicio(), periodo.getFim());
		if (lista.isEmpty()) {
			return "";
		}
		String datasTxt = lista.stream().map(r -> "%s (%d)".formatted(r.getData().format(DIA_MES), r.getQuantidade()))
				.collect(Collectors.joining(", "));
		return "Datas: " + datasTxt;
	}

	private int semestreAtual(List<Materia> todas) {
		LocalDate hoje = LocalDate.now(clock);
		return periodos.findFirstByBimestreIsNullAndInicioLessThanEqualAndFimGreaterThanEqual(hoje, hoje)
				.map(Periodo::getSemestre)
				.orElseGet(() -> todas.stream().mapToInt(Materia::getSemestre).max().orElse(1));
	}

	// ---------------------------------------------------------------- simular

	private String simular(String t) {
		LocalDate hoje = LocalDate.now(clock);
		List<LocalDate> dias = datas.varias(t, hoje, true);
		if (dias.isEmpty()) {
			return "Quais dias? Ex.: \"Vou faltar nos dias 10, 12 e 15 de maio\".";
		}
		List<Materia> filtro = acharMaterias(t);
		Simulacao sim = frequencia.simular(dias);

		StringBuilder resposta = new StringBuilder("Simulação (nada foi registrado):\n");
		for (DiaSemAula d : sim.diasSemAula()) {
			resposta.append("- %s: %s%n".formatted(descrever(d.data()), d.motivo()));
		}
		List<LinhaSimulacao> linhas = sim.linhas().stream()
				.filter(l -> filtro.isEmpty() || filtro.stream().anyMatch(m -> m.getId().equals(l.materia().getId())))
				.toList();
		if (linhas.isEmpty()) {
			resposta.append("Nenhuma aula nova nesses dias.");
			return resposta.toString();
		}
		for (LinhaSimulacao l : linhas) {
			Materia m = l.materia();
			String dias2 = l.datas().stream().map(d -> d.format(DIA_MES)).collect(Collectors.joining(", "));
			resposta.append("- %s (%s): +%d → %s (hoje: %d)%n".formatted(m.getNome(), dias2, l.adicional(),
					situacao(l.depois(), m.getLimiteFaltas()), l.atual()));
		}
		return resposta.toString().stripTrailing();
	}

	// ---------------------------------------------------------------- apoio

	/** Matérias citadas na mensagem. Nome contido na frase vence; senão, maior sobreposição de palavras. */
	List<Materia> acharMaterias(String t) {
		List<Materia> todas = materias.findAll();
		String frase = " " + t + " ";

		Materia exata = null;
		int tamanho = 0;
		for (Materia m : todas) {
			String nome = Texto.normalizar(m.getNome());
			if (frase.contains(" " + nome + " ") && nome.length() > tamanho) {
				exata = m;
				tamanho = nome.length();
			}
		}
		if (exata != null) {
			return List.of(exata);
		}

		Set<String> palavras = Set.of(t.split(" "));
		double melhor = 0.5;
		List<Materia> melhores = new ArrayList<>();
		for (Materia m : todas) {
			List<String> chave = List.of(Texto.normalizar(m.getNome()).split(" ")).stream()
					.filter(p -> !PALAVRAS_VAZIAS.contains(p)).toList();
			if (chave.isEmpty()) {
				continue;
			}
			double score = (double) chave.stream().filter(palavras::contains).count() / chave.size();
			if (score > melhor) {
				melhor = score;
				melhores.clear();
				melhores.add(m);
			} else if (score == melhor && score > 0.5) {
				melhores.add(m);
			}
		}
		return melhores;
	}

	private String ambiguidade(List<Materia> achadas) {
		return "Encontrei mais de uma matéria: %s. Qual delas?"
				.formatted(achadas.stream().map(Materia::getNome).collect(Collectors.joining(", ")));
	}

	/** Lê "segundo bimestre", "2º bimestre", "bimestre 2", "quinto semestre" etc. */
	private Integer ordinal(String t, String unidade) {
		Matcher antes = Pattern.compile("\\b" + ORDINAL + "\\s*o?\\s+" + unidade + "\\b").matcher(t);
		if (antes.find()) {
			return paraNumero(antes.group(1));
		}
		Matcher depois = Pattern.compile("\\b" + unidade + "\\s+(\\d{1,2})\\b").matcher(t);
		return depois.find() ? Integer.valueOf(depois.group(1)) : null;
	}

	private Integer paraNumero(String valor) {
		if (Character.isDigit(valor.charAt(0))) {
			return Integer.valueOf(valor);
		}
		return ORDINAIS.get(valor.substring(0, valor.length() - 1));
	}

	private String descrever(LocalDate data) {
		return "%s, %s".formatted(FrequenciaService.nomeDia(data.getDayOfWeek()),
				data.getYear() == LocalDate.now(clock).getYear() ? data.format(DIA_MES) : data.format(DATA_COMPLETA));
	}

	private static String faltas(long n) {
		return n + (n == 1 ? " falta" : " faltas");
	}

	private static String situacao(long faltas, int limite) {
		if (limite <= 0) {
			return faltas(faltas);
		}
		long pct = Math.round(100.0 * faltas / limite);
		String s = "%d/%d (%d%%)".formatted(faltas, limite, pct);
		if (faltas > limite) {
			return s + " ⚠️ limite ultrapassado";
		}
		if (faltas == limite) {
			return s + " ⚠️ no limite";
		}
		if (pct >= 75) {
			return s + " ⚠️ atenção";
		}
		return s;
	}
}
