package br.com.sistemas.chamados;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import br.com.sistemas.chamados.controller.ChamadoController;
import br.com.sistemas.chamados.dto.AlterarStatusRequest;
import br.com.sistemas.chamados.dto.ChamadoRequest;
import br.com.sistemas.chamados.dto.ChamadoResponse;
import br.com.sistemas.chamados.exception.RecursoNaoEncontradoException;
import br.com.sistemas.chamados.exception.RegraNegocioException;
import br.com.sistemas.chamados.service.ChamadoService;

@WebMvcTest(controllers = ChamadoController.class)
@Import(ChamadoControllerTests.MockConfig.class)
class ChamadoControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ChamadoService service;

    private ChamadoResponse chamado(String status) {
        return new ChamadoResponse(1L, "Erro", "Tela fora do ar", LocalDateTime.now(), null,
            "ALTA", status, 1L, 1L, null, List.of());
    }

    @Test
    void deveCriarChamadoCom201ELocation() throws Exception {
        when(service.criar(any(ChamadoRequest.class))).thenReturn(chamado("ABERTO"));

        mockMvc.perform(post("/chamados")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"titulo":"Erro","descricao":"Tela fora do ar","prioridade":"ALTA",
                    "clienteId":1,"categoriaId":1}
                    """))
            .andExpect(status().isCreated())
            .andExpect(header().string("Location", "/chamados/1"))
            .andExpect(jsonPath("$.status").value("ABERTO"));
    }

    @Test
    void deveRetornar400QuandoCamposObrigatoriosFaltam() throws Exception {
        mockMvc.perform(post("/chamados")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"titulo\":\"\",\"descricao\":\"\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.erro").value("Dados inválidos"))
            .andExpect(jsonPath("$.detalhes").isArray());
    }

    @Test
    void deveRetornar404QuandoChamadoNaoExiste() throws Exception {
        when(service.buscar(999L))
            .thenThrow(new RecursoNaoEncontradoException("Chamado 999 não encontrado"));

        mockMvc.perform(get("/chamados/999"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.erro").value("Chamado 999 não encontrado"));
    }

    @Test
    void deveRetornar409ParaTransicaoInvalida() throws Exception {
        when(service.alterarStatus(eq(1L), any(AlterarStatusRequest.class)))
            .thenThrow(new RegraNegocioException("Transição de status inválida"));

        mockMvc.perform(patch("/chamados/1/status")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"FECHADO\"}"))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    void deveAlterarStatus() throws Exception {
        when(service.alterarStatus(eq(1L), any(AlterarStatusRequest.class)))
            .thenReturn(chamado("EM_ATENDIMENTO"));

        mockMvc.perform(patch("/chamados/1/status")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"EM_ATENDIMENTO\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("EM_ATENDIMENTO"));
    }

    @TestConfiguration
    static class MockConfig {
        @Bean
        ChamadoService chamadoService() {
            return Mockito.mock(ChamadoService.class);
        }
    }
}
