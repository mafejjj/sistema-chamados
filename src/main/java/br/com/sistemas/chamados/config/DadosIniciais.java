package br.com.sistemas.chamados.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import br.com.sistemas.chamados.dto.AlterarStatusRequest;
import br.com.sistemas.chamados.dto.AtribuirTecnicoRequest;
import br.com.sistemas.chamados.dto.CategoriaRequest;
import br.com.sistemas.chamados.dto.CategoriaResponse;
import br.com.sistemas.chamados.dto.ChamadoRequest;
import br.com.sistemas.chamados.dto.ChamadoResponse;
import br.com.sistemas.chamados.dto.ClienteRequest;
import br.com.sistemas.chamados.dto.ClienteResponse;
import br.com.sistemas.chamados.dto.ComentarioRequest;
import br.com.sistemas.chamados.dto.TecnicoRequest;
import br.com.sistemas.chamados.dto.TecnicoResponse;
import br.com.sistemas.chamados.entity.Perfil;
import br.com.sistemas.chamados.entity.Prioridade;
import br.com.sistemas.chamados.entity.StatusChamado;
import br.com.sistemas.chamados.repository.ClienteRepository;
import br.com.sistemas.chamados.service.CategoriaService;
import br.com.sistemas.chamados.service.ChamadoService;
import br.com.sistemas.chamados.service.ClienteService;
import br.com.sistemas.chamados.service.ComentarioService;
import br.com.sistemas.chamados.service.TecnicoService;

/**
 * Popula o banco com dados de demonstração.
 * Ative com o profile "demo": mvnw spring-boot:run "-Dspring-boot.run.profiles=demo"
 * Os dados passam pelos Services, então todas as regras de negócio são respeitadas.
 */
@Component
@Profile("demo")
public class DadosIniciais implements CommandLineRunner {

    private final ClienteRepository clienteRepository;
    private final ClienteService clienteService;
    private final CategoriaService categoriaService;
    private final TecnicoService tecnicoService;
    private final ChamadoService chamadoService;
    private final ComentarioService comentarioService;

    public DadosIniciais(ClienteRepository clienteRepository, ClienteService clienteService,
        CategoriaService categoriaService, TecnicoService tecnicoService,
        ChamadoService chamadoService, ComentarioService comentarioService) {
        this.clienteRepository = clienteRepository;
        this.clienteService = clienteService;
        this.categoriaService = categoriaService;
        this.tecnicoService = tecnicoService;
        this.chamadoService = chamadoService;
        this.comentarioService = comentarioService;
    }

    @Override
    public void run(String... args) {
        if (clienteRepository.count() > 0) {
            return;
        }

        ClienteResponse ana = clienteService.criar(
            new ClienteRequest("Ana Souza", "ana@exemplo.com", "47999999999"));
        ClienteResponse joao = clienteService.criar(
            new ClienteRequest("João Silva", "joao@exemplo.com", "47988888888"));

        CategoriaResponse software = categoriaService.criar(
            new CategoriaRequest("Software", "Problemas em sistemas e aplicativos"));
        CategoriaResponse rede = categoriaService.criar(
            new CategoriaRequest("Rede", "Conectividade e internet"));
        CategoriaResponse hardware = categoriaService.criar(
            new CategoriaRequest("Hardware", "Equipamentos e periféricos"));

        tecnicoService.criar(new TecnicoRequest("Maria Admin", "admin@exemplo.com", Perfil.ADMIN));
        TecnicoResponse carlos = tecnicoService.criar(
            new TecnicoRequest("Carlos Lima", "carlos@exemplo.com", Perfil.TECNICO));

        chamadoService.criar(new ChamadoRequest("Sistema fora do ar",
            "Tela de login não carrega", Prioridade.CRITICA, ana.id(), software.id()));

        ChamadoResponse wifi = chamadoService.criar(new ChamadoRequest("Wi-Fi instável",
            "Conexão cai a cada 10 minutos", Prioridade.ALTA, ana.id(), rede.id()));
        chamadoService.atribuir(wifi.id(), new AtribuirTecnicoRequest(carlos.id()));
        chamadoService.alterarStatus(wifi.id(),
            new AlterarStatusRequest(StatusChamado.EM_ATENDIMENTO));
        comentarioService.criar(wifi.id(),
            new ComentarioRequest("Roteador será substituído", "Carlos Lima"));

        ChamadoResponse mouse = chamadoService.criar(new ChamadoRequest("Mouse com defeito",
            "Botão esquerdo não funciona", Prioridade.BAIXA, joao.id(), hardware.id()));
        chamadoService.atribuir(mouse.id(), new AtribuirTecnicoRequest(carlos.id()));
        chamadoService.alterarStatus(mouse.id(),
            new AlterarStatusRequest(StatusChamado.EM_ATENDIMENTO));
        chamadoService.alterarStatus(mouse.id(), new AlterarStatusRequest(StatusChamado.FECHADO));
    }
}
