package br.com.example.chamados.service;

/** Violação de regra de negócio; a mensagem é segura para ser mostrada ao usuário. */
public class RegraNegocioException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	public RegraNegocioException(String mensagem) {
		super(mensagem);
	}
}
