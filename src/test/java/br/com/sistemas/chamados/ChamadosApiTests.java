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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
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
import br.com.sistemas.chamados.repository.HistoricoStatusRepository;
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

    @Autowired
    private HistoricoStatusRepository historicoRepository;

    @BeforeEach
    void limparBanco() {
        historicoRepository.deleteAll();
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

    @Test
    void deveCompletarCrudDeClienteEImpedirEmailDuplicado() throws Exception {
        String email = "cliente-crud@exemplo.com";
        String resposta = mockMvc.perform(post("/clientes")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"nome":"Cliente CRUD","email":"cliente-crud@exemplo.com",
                    "telefone":"47999999999"}
                    """))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();
        Number idNumero = com.jayway.jsonpath.JsonPath.read(resposta, "$.id");
        Long id = idNumero.longValue();

        mockMvc.perform(get("/clientes"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].email").value(email));
        mockMvc.perform(get("/clientes/{id}", id))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.nome").value("Cliente CRUD"));
        mockMvc.perform(put("/clientes/{id}", id)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"nome":"Cliente Atualizado","email":"cliente-crud@exemplo.com",
                    "telefone":"47888888888"}
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.nome").value("Cliente Atualizado"));
        mockMvc.perform(post("/clientes")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"nome":"Outro","email":"cliente-crud@exemplo.com","telefone":"47777777777"}
                    """))
            .andExpect(status().isConflict());
        mockMvc.perform(delete("/clientes/{id}", id))
            .andExpect(status().isNoContent());
        mockMvc.perform(get("/clientes/{id}", id))
            .andExpect(status().isNotFound());
    }

    @Test
    void deveCompletarCrudDeCategoria() throws Exception {
        String resposta = mockMvc.perform(post("/categorias")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"nome":"Categoria CRUD","descricao":"Descrição"}
                    """))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();
        Number idNumero = com.jayway.jsonpath.JsonPath.read(resposta, "$.id");
        Long id = idNumero.longValue();

        mockMvc.perform(get("/categorias"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].nome").value("Categoria CRUD"));
        mockMvc.perform(get("/categorias/{id}", id))
            .andExpect(status().isOk());
        mockMvc.perform(put("/categorias/{id}", id)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"nome":"Categoria Atualizada","descricao":"Nova descrição"}
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.nome").value("Categoria Atualizada"));
        mockMvc.perform(post("/categorias")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"nome":"Categoria Atualizada","descricao":"Duplicada"}
                    """))
            .andExpect(status().isConflict());
        mockMvc.perform(delete("/categorias/{id}", id))
            .andExpect(status().isNoContent());
    }

    @Test
    void deveCompletarCrudDeTecnico() throws Exception {
        String resposta = mockMvc.perform(post("/tecnicos")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"nome":"Técnico CRUD","email":"tecnico-crud@exemplo.com","perfil":"ADMIN"}
                    """))
            .andExpect(status().isCreated())
            .andReturn().getResponse().getContentAsString();
        Number idNumero = com.jayway.jsonpath.JsonPath.read(resposta, "$.id");
        Long id = idNumero.longValue();

        mockMvc.perform(get("/tecnicos"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].perfil").value("ADMIN"));
        mockMvc.perform(get("/tecnicos/{id}", id))
            .andExpect(status().isOk());
        mockMvc.perform(put("/tecnicos/{id}", id)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"nome":"Técnico Atualizado","email":"tecnico-crud@exemplo.com",
                    "perfil":"TECNICO"}
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.perfil").value("TECNICO"));
        mockMvc.perform(delete("/tecnicos/{id}", id))
            .andExpect(status().isNoContent());
    }

    @Test
    void deveValidarTransicaoInvalidaEPaginacaoEChamadosDoCliente() throws Exception {
        Cliente cliente = criarCliente("cliente-paginacao@exemplo.com");
        Categoria categoria = criarCategoria("Paginacao");
        Long chamadoId = criarChamado(cliente, categoria);

        mockMvc.perform(patch("/chamados/{id}/status", chamadoId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"FECHADO\"}"))
            .andExpect(status().isConflict());
        mockMvc.perform(get("/clientes/{id}/chamados", cliente.getId())
                .param("page", "0").param("size", "1")
                .param("sort", "dataAbertura,desc"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content", hasSize(1)))
            .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void deveRegistrarHistoricoDeStatus() throws Exception {
        Cliente cliente = criarCliente("cliente-historico@exemplo.com");
        Categoria categoria = criarCategoria("Historico");
        Long chamadoId = criarChamado(cliente, categoria);

        mockMvc.perform(patch("/chamados/{id}/status", chamadoId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"EM_ATENDIMENTO\"}"))
            .andExpect(status().isOk());
        mockMvc.perform(patch("/chamados/{id}/status", chamadoId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"FECHADO\"}"))
            .andExpect(status().isOk());

        mockMvc.perform(get("/chamados/{id}/historico", chamadoId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(3)))
            .andExpect(jsonPath("$[0].statusAnterior").doesNotExist())
            .andExpect(jsonPath("$[0].statusNovo").value("ABERTO"))
            .andExpect(jsonPath("$[1].statusAnterior").value("ABERTO"))
            .andExpect(jsonPath("$[1].statusNovo").value("EM_ATENDIMENTO"))
            .andExpect(jsonPath("$[2].statusNovo").value("FECHADO"));

        mockMvc.perform(get("/chamados/{id}/historico", 99999))
            .andExpect(status().isNotFound());
    }

    @Test
    void deveExcluirChamadoComComentariosEHistorico() throws Exception {
        Cliente cliente = criarCliente("cliente-excluir@exemplo.com");
        Categoria categoria = criarCategoria("Excluir");
        Long chamadoId = criarChamado(cliente, categoria);

        mockMvc.perform(post("/chamados/{id}/comentarios", chamadoId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"texto\":\"Comentário\",\"autor\":\"Ana\"}"))
            .andExpect(status().isCreated());

        mockMvc.perform(delete("/chamados/{id}", chamadoId))
            .andExpect(status().isNoContent());
        mockMvc.perform(get("/chamados/{id}", chamadoId))
            .andExpect(status().isNotFound());
    }

    @Test
    void deveResumirChamadosNoDashboard() throws Exception {
        Cliente cliente = criarCliente("cliente-dashboard@exemplo.com");
        Categoria categoria = criarCategoria("Dashboard");
        Long chamadoId = criarChamado(cliente, categoria);
        criarChamado(cliente, categoria);

        mockMvc.perform(patch("/chamados/{id}/status", chamadoId)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"EM_ATENDIMENTO\"}"))
            .andExpect(status().isOk());

        mockMvc.perform(get("/dashboard"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalChamados").value(2))
            .andExpect(jsonPath("$.porStatus.ABERTO").value(1))
            .andExpect(jsonPath("$.porStatus.EM_ATENDIMENTO").value(1))
            .andExpect(jsonPath("$.porStatus.FECHADO").value(0))
            .andExpect(jsonPath("$.porPrioridade.ALTA").value(2))
            .andExpect(jsonPath("$.porPrioridade.BAIXA").value(0));
    }

    @Test
    void deveRetornar400ParaEnumInvalidoNoCorpoENoFiltro() throws Exception {
        mockMvc.perform(post("/tecnicos")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nome\":\"X\",\"email\":\"x@exemplo.com\",\"perfil\":\"GERENTE\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400));

        mockMvc.perform(get("/chamados").param("status", "INEXISTENTE"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.erro").value("Parâmetro inválido"));
    }

    @Test
    void deveExporDocumentacaoOpenApi() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.info.title").value("Sistema de Chamados API"))
            .andExpect(jsonPath("$.paths['/chamados']").exists());
    }

    @Test
    void deveServirAsTelasDoFrontEnd() throws Exception {
        for (String tela : java.util.List.of("/index.html", "/chamados.html", "/chamado.html",
                "/clientes.html", "/cliente.html", "/categorias.html", "/tecnicos.html",
                "/js/app.js", "/js/crud.js", "/css/estilo.css")) {
            mockMvc.perform(get(tela)).andExpect(status().isOk());
        }
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
