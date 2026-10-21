package br.com.example.chamados.chat;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

/**
 * Extrai datas de mensagens já normalizadas (ver {@link Texto#normalizar}).
 * Entende: hoje, ontem, anteontem, amanhã, 10/05, 10/05/2026, "dia 10", "dias 10, 12 e 15 de maio".
 */
@Component
class DataParser {

	private static final Pattern COMPLETA = Pattern.compile("\\b(\\d{1,2})/(\\d{1,2})(?:/(\\d{4}|\\d{2}))?\\b");
	private static final Pattern LISTA_DIAS = Pattern
			.compile("\\bdias?\\s+(\\d{1,2}(?:\\s*(?:,|e)\\s*\\d{1,2})*)\\b");
	private static final Pattern MES = Pattern.compile(
			"\\bde\\s+(janeiro|fevereiro|marco|abril|maio|junho|julho|agosto|setembro|outubro|novembro|dezembro)\\b");
	private static final List<String> MESES = List.of("janeiro", "fevereiro", "marco", "abril", "maio", "junho",
			"julho", "agosto", "setembro", "outubro", "novembro", "dezembro");

	/** Data única da mensagem: palavras relativas primeiro, depois datas numéricas. */
	Optional<LocalDate> unica(String t, LocalDate hoje, boolean futuro) {
		return relativa(t, hoje).or(() -> numericas(t, hoje, futuro).stream().findFirst());
	}

	/** Todas as datas da mensagem, ordenadas e sem repetição. */
	List<LocalDate> varias(String t, LocalDate hoje, boolean futuro) {
		List<LocalDate> datas = numericas(t, hoje, futuro);
		if (datas.isEmpty()) {
			return relativa(t, hoje).map(List::of).orElse(List.of());
		}
		return datas;
	}

	Optional<LocalDate> relativa(String t, LocalDate hoje) {
		if (t.matches(".*\\bdepois de amanha\\b.*")) {
			return Optional.of(hoje.plusDays(2));
		}
		if (t.matches(".*\\bamanha\\b.*")) {
			return Optional.of(hoje.plusDays(1));
		}
		if (t.matches(".*\\banteontem\\b.*")) {
			return Optional.of(hoje.minusDays(2));
		}
		if (t.matches(".*\\bontem\\b.*")) {
			return Optional.of(hoje.minusDays(1));
		}
		if (t.matches(".*\\b(hoje|hj)\\b.*")) {
			return Optional.of(hoje);
		}
		return Optional.empty();
	}

	private List<LocalDate> numericas(String t, LocalDate hoje, boolean futuro) {
		List<LocalDate> datas = new ArrayList<>();

		Matcher completa = COMPLETA.matcher(t);
		while (completa.find()) {
			int dia = Integer.parseInt(completa.group(1));
			int mes = Integer.parseInt(completa.group(2));
			String anoTxt = completa.group(3);
			try {
				if (anoTxt != null) {
					int ano = Integer.parseInt(anoTxt);
					datas.add(LocalDate.of(ano < 100 ? 2000 + ano : ano, mes, dia));
				} else {
					datas.add(ajustarAno(LocalDate.of(hoje.getYear(), mes, dia), hoje, futuro));
				}
			} catch (DateTimeException e) {
				// data inválida (ex.: 31/02): ignora
			}
		}
		if (!datas.isEmpty()) {
			return ordenar(datas);
		}

		Matcher lista = LISTA_DIAS.matcher(t);
		if (lista.find()) {
			Matcher mes = MES.matcher(t);
			Integer mesNomeado = mes.find() ? MESES.indexOf(mes.group(1)) + 1 : null;
			for (String dia : lista.group(1).split("\\D+")) {
				if (dia.isBlank()) {
					continue;
				}
				int d = Integer.parseInt(dia);
				try {
					if (mesNomeado != null) {
						datas.add(ajustarAno(LocalDate.of(hoje.getYear(), mesNomeado, d), hoje, futuro));
					} else {
						datas.add(diaDoMesProximo(d, hoje, futuro));
					}
				} catch (DateTimeException e) {
					// dia inexistente no mês: ignora
				}
			}
		}
		return ordenar(datas);
	}

	/** Sem mês informado: no futuro, dia já passado vira o próximo mês; no passado, dia futuro vira o mês anterior. */
	private LocalDate diaDoMesProximo(int dia, LocalDate hoje, boolean futuro) {
		YearMonth ym = YearMonth.from(hoje);
		if (futuro && dia < hoje.getDayOfMonth()) {
			ym = ym.plusMonths(1);
		} else if (!futuro && dia > hoje.getDayOfMonth()) {
			ym = ym.minusMonths(1);
		}
		return ym.atDay(dia);
	}

	private LocalDate ajustarAno(LocalDate data, LocalDate hoje, boolean futuro) {
		if (futuro && data.isBefore(hoje)) {
			return data.plusYears(1);
		}
		if (!futuro && data.isAfter(hoje)) {
			return data.minusYears(1);
		}
		return data;
	}

	private List<LocalDate> ordenar(List<LocalDate> datas) {
		return datas.stream().distinct().sorted().toList();
	}
}
