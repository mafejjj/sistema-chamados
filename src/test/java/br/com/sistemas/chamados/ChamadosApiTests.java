package br.com.sistemas.chamados;

import static org.hamcrest.Matchers.hasSize;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.sistemas.chamados.entity.Categoria;
import br.com.sistemas.chamados.entity.Cliente;
import br.com.sistemas.chamados.entity.Perfil;
import br.com.sistemas.chamados.entity.Tecnico;
import br.com.sistemas.chamados.repository.CategoriaRepository;
import br.com.sistemas.chamados.repository.ChamadoRepository;
import br.com.sistemas.chamados.repository.ClienteRepository;
import br.com.sistemas.chamados.repository.ComentarioRepository;
import br.com.sistemas.chamados.repository.TecnicoRepository;

@SpringBootTest
@AutoConfigureMockMvc
class ChamadosApiTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private CategoriaRepository categoriaRepository;

    @Autowired
    private TecnicoRepository tecnicoRepository;

    @Autowired
    private ChamadoRepository chamadoRepository;

    @Autowired
    private ComentarioRepository comentarioRepository;

    @BeforeEach
    void limparBanco() {
        comentarioRepository.deleteAll();
        chamadoRepository.deleteAll();
        tecnicoRepository.deleteAll();
        categoriaRepository.deleteAll();
        clienteRepository.deleteAll();
    }

    @Test
    void deveCriarChamadoAbertoEListarComentarioNoDetalhe() throws Exception {
        Cliente cliente = criarCliente("cliente-api@exemplo.com");
        Categoria categoria = criarCategoria("Software");

        mockMvc.perform(post("/chamados")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"titulo":"Erro no sistema","descricao":"Tela indisponível",
                    "prioridade":"ALTA","clienteId":%d,"categoriaId":%d}
                    """.formatted(cliente.getId(), categoria.getId())))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.status").value("ABERTO"))
            .andExpect(jsonPath("$.dataAbertura").isNotEmpty());

        Long chamadoId = chamadoRepository.findAll().get(0).getId();

        mockMvc.perform(post("/chamados/{id}/comentarios", chamadoId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"texto":"Estamos analisando o problema","autor":"Ana"}
                    """))
            .andExpect(status().isCreated());

        mockMvc.perform(get("/chamados/{id}", chamadoId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.comentarios", hasSize(1)))
            .andExpect(jsonPath("$.comentarios[0].texto")
                .value("Estamos analisando o problema"));
    }

    @Test
    void deveImpedirAlteracaoDeChamadoFechado() throws Exception {
        Cliente cliente = criarCliente("cliente-status@exemplo.com");
        Categoria categoria = criarCategoria("Rede");
        Tecnico tecnico = criarTecnico("tecnico-status@exemplo.com");
        Long chamadoId = criarChamado(cliente, categoria);

        mockMvc.perform(patch("/chamados/{id}/atribuir", chamadoId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"tecnicoId\":%d}".formatted(tecnico.getId())))
            .andExpect(status().isOk());

        mockMvc.perform(patch("/chamados/{id}/status", chamadoId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"EM_ATENDIMENTO\"}"))
            .andExpect(status().isOk());

        mockMvc.perform(patch("/chamados/{id}/status", chamadoId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"FECHADO\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.dataFechamento").isNotEmpty());

        mockMvc.perform(put("/chamados/{id}", chamadoId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"titulo":"Alteração","descricao":"Não permitida",
                    "prioridade":"BAIXA","clienteId":%d,"categoriaId":%d}
                    """.formatted(cliente.getId(), categoria.getId())))
            .andExpect(status().isConflict());
    }

    @Test
    void deveFiltrarChamadosPorStatusPrioridadeECliente() throws Exception {
        Cliente cliente = criarCliente("cliente-filtro@exemplo.com");
        Categoria categoria = criarCategoria("Hardware");
        criarChamado(cliente, categoria);

        mockMvc.perform(get("/chamados")
                .param("status", "ABERTO")
                .param("prioridade", "ALTA")
                .param("clienteId", cliente.getId().toString()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content", hasSize(1)))
            .andExpect(jsonPath("$.content[0].clienteId").value(cliente.getId()));
    }

    @Test
    void deveValidarDadosERelacionamentosDoChamado() throws Exception {
        mockMvc.perform(post("/chamados")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"titulo":"","descricao":"","prioridade":null,
                    "clienteId":9999,"categoriaId":9999}
                    """))
            .andExpect(status().isBadRequest());

        Cliente cliente = criarCliente("cliente-404@exemplo.com");
        mockMvc.perform(post("/chamados")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"titulo":"Chamado","descricao":"Descrição","prioridade":"BAIXA",
                    "clienteId":%d,"categoriaId":9999}
                    """.formatted(cliente.getId())))
            .andExpect(status().isNotFound());
    }

    @Test
    void deveImpedirEmailDuplicadoDeTecnico() throws Exception {
        mockMvc.perform(post("/tecnicos")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"nome":"Técnico 1","email":"duplicado@exemplo.com","perfil":"TECNICO"}
                    """))
            .andExpect(status().isCreated());

        mockMvc.perform(post("/tecnicos")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"nome":"Técnico 2","email":"duplicado@exemplo.com","perfil":"ADMIN"}
                    """))
            .andExpect(status().isConflict());
    }

    private Cliente criarCliente(String email) {
        return clienteRepository.save(new Cliente("Cliente de teste", email, "47999999999"));
    }

    private Categoria criarCategoria(String nome) {
        return categoriaRepository.save(new Categoria(nome, "Categoria de teste"));
    }

    private Tecnico criarTecnico(String email) {
        return tecnicoRepository.save(new Tecnico("Técnico de teste", email, Perfil.TECNICO));
    }

    private Long criarChamado(Cliente cliente, Categoria categoria) throws Exception {
        mockMvc.perform(post("/chamados")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"titulo":"Chamado de teste","descricao":"Descrição de teste",
                    "prioridade":"ALTA","clienteId":%d,"categoriaId":%d}
                    """.formatted(cliente.getId(), categoria.getId())))
            .andExpect(status().isCreated());
        return chamadoRepository.findAll().get(0).getId();
    }
}
