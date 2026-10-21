package br.com.example.chamados.chat;

import java.text.Normalizer;
import java.util.Locale;

final class Texto {

	private Texto() {
	}

	/** Minúsculas, sem acentos e sem pontuação (mantém letras, números, "/" e ","). */
	static String normalizar(String texto) {
		String n = Normalizer.normalize(texto, Normalizer.Form.NFKD).replaceAll("\\p{M}", "");
		n = n.toLowerCase(Locale.ROOT).replace('°', 'o');
		n = n.replaceAll("[^a-z0-9/, ]", " ");
		return n.replaceAll("\\s+", " ").trim();
	}
}
