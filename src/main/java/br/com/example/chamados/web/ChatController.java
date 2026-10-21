package br.com.example.chamados.web;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.example.chamados.chat.ChatService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Endpoint único do chat. Um futuro webhook do WhatsApp chamaria o mesmo {@link ChatService}. */
@RestController
@RequestMapping("/api/chat")
public class ChatController {

	public record Pergunta(@NotBlank @Size(max = 500) String mensagem) {
	}

	public record Resposta(String resposta) {
	}

	private final ChatService chat;

	public ChatController(ChatService chat) {
		this.chat = chat;
	}

	@PostMapping
	public Resposta conversar(@Valid @RequestBody Pergunta pergunta) {
		return new Resposta(chat.responder(pergunta.mensagem()));
	}
}
