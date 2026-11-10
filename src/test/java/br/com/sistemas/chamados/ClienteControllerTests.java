package br.com.sistemas.chamados;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import br.com.sistemas.chamados.controller.ClienteController;
import br.com.sistemas.chamados.dto.ClienteRequest;
import br.com.sistemas.chamados.dto.ClienteResponse;
import br.com.sistemas.chamados.exception.RecursoNaoEncontradoException;
import br.com.sistemas.chamados.service.ClienteService;

@WebMvcTest(controllers = ClienteController.class)
@Import(ClienteControllerTests.MockConfig.class)
class ClienteControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ClienteService service;

    @Test
    void deveCriarClienteCom201ELocation() throws Exception {
        when(service.criar(any(ClienteRequest.class)))
            .thenReturn(new ClienteResponse(1L, "Ana", "ana@exemplo.com", "47999999999"));

        mockMvc.perform(post("/clientes")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"nome":"Ana","email":"ana@exemplo.com","telefone":"47999999999"}
                    """))
            .andExpect(status().isCreated())
            .andExpect(header().string("Location", "/clientes/1"))
            .andExpect(jsonPath("$.nome").value("Ana"));
    }

    @Test
    void deveRetornar400ParaDadosInvalidos() throws Exception {
        mockMvc.perform(post("/clientes")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"nome":"","email":"invalido","telefone":"123"}
                    """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.detalhes").isArray());
    }

    @Test
    void deveRetornar404QuandoClienteNaoExiste() throws Exception {
        when(service.buscar(999L))
            .thenThrow(new RecursoNaoEncontradoException("Cliente 999 não encontrado"));

        mockMvc.perform(get("/clientes/999"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void deveConsultarAtualizarEExcluirCliente() throws Exception {
        when(service.buscar(1L))
            .thenReturn(new ClienteResponse(1L, "Ana", "ana@exemplo.com", "47999999999"));
        when(service.atualizar(any(Long.class), any(ClienteRequest.class)))
            .thenReturn(new ClienteResponse(1L, "Ana Atualizada",
                "ana.atualizada@exemplo.com", "47888888888"));

        mockMvc.perform(get("/clientes/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.email").value("ana@exemplo.com"));

        mockMvc.perform(put("/clientes/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"nome":"Ana Atualizada","email":"ana.atualizada@exemplo.com",
                    "telefone":"47888888888"}
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.nome").value("Ana Atualizada"));

        mockMvc.perform(delete("/clientes/1"))
            .andExpect(status().isNoContent());
    }

    @Test
    void deveListarClientes() throws Exception {
        when(service.listar()).thenReturn(java.util.List.of(
            new ClienteResponse(1L, "Ana", "ana@exemplo.com", "47999999999")));

        mockMvc.perform(get("/clientes"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", org.hamcrest.Matchers.hasSize(1)))
            .andExpect(jsonPath("$[0].id").value(1));
    }

    @TestConfiguration
    static class MockConfig {
        @Bean
        ClienteService clienteService() {
            return org.mockito.Mockito.mock(ClienteService.class);
        }
    }
}
