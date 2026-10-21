package br.com.example.chamados.web;

import java.util.Map;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import br.com.example.chamados.service.RegraNegocioException;

@RestControllerAdvice
public class ApiExceptionHandler {

	@ExceptionHandler(RegraNegocioException.class)
	@ResponseStatus(HttpStatus.BAD_REQUEST)
	public Map<String, String> regraNegocio(RegraNegocioException e) {
		return Map.of("erro", e.getMessage());
	}

	@ExceptionHandler(DataIntegrityViolationException.class)
	@ResponseStatus(HttpStatus.CONFLICT)
	public Map<String, String> integridade(DataIntegrityViolationException e) {
		return Map.of("erro", "Registro duplicado ou ainda em uso por outro cadastro.");
	}
}
