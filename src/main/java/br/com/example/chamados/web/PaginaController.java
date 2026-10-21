package br.com.example.chamados.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PaginaController {

	@GetMapping("/")
	public String chat() {
		return "chat";
	}
}
