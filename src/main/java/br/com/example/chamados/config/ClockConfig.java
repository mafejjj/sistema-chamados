package br.com.example.chamados.config;

import java.time.Clock;
import java.time.ZoneId;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ClockConfig {

	/** "Hoje" e "ontem" são calculados no fuso de Brasília, independente do fuso do servidor. */
	@Bean
	public Clock clock() {
		return Clock.system(ZoneId.of("America/Sao_Paulo"));
	}
}
