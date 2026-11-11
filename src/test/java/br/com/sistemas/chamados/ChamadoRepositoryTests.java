package br.com.sistemas.chamados;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import br.com.sistemas.chamados.entity.Categoria;
import br.com.sistemas.chamados.entity.Chamado;
import br.com.sistemas.chamados.entity.Cliente;
import br.com.sistemas.chamados.entity.Prioridade;
import br.com.sistemas.chamados.entity.StatusChamado;
import br.com.sistemas.chamados.repository.CategoriaRepository;
import br.com.sistemas.chamados.repository.ChamadoRepository;
import br.com.sistemas.chamados.repository.ClienteRepository;

@DataJpaTest
class ChamadoRepositoryTests {

    @Autowired
    private ChamadoRepository chamadoRepository;

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private CategoriaRepository categoriaRepository;

    private Cliente ana;
    private Cliente joao;
    private Categoria categoria;

    @BeforeEach
    void preparar() {
        ana = clienteRepository.save(new Cliente("Ana", "ana@exemplo.com", "47999999999"));
        joao = clienteRepository.save(new Cliente("João", "joao@exemplo.com", "47988888888"));
        categoria = categoriaRepository.save(new Categoria("Software", "Aplicativos"));

        chamadoRepository.save(new Chamado("A", "Desc", Prioridade.ALTA, ana, categoria));
        chamadoRepository.save(new Chamado("B", "Desc", Prioridade.BAIXA, ana, categoria));
        Chamado emAtendimento = new Chamado("C", "Desc", Prioridade.ALTA, joao, categoria);
        emAtendimento.setStatus(StatusChamado.EM_ATENDIMENTO);
        chamadoRepository.save(emAtendimento);
    }

    @Test
    void novoChamadoNasceAbertoComDataDeAbertura() {
        Chamado salvo = chamadoRepository.save(
            new Chamado("Novo", "Desc", Prioridade.MEDIA, ana, categoria));

        Chamado encontrado = chamadoRepository.findById(salvo.getId()).orElseThrow();
        assertThat(encontrado.getStatus()).isEqualTo(StatusChamado.ABERTO);
        assertThat(encontrado.getDataAbertura()).isNotNull();
        assertThat(encontrado.getDataFechamento()).isNull();
        assertThat(encontrado.getCliente().getId()).isEqualTo(ana.getId());
    }

    @Test
    void deveFiltrarPorStatusPrioridadeECliente() {
        PageRequest pagina = PageRequest.of(0, 10);

        assertThat(chamadoRepository.findByStatus(StatusChamado.ABERTO, pagina)
            .getTotalElements()).isEqualTo(2);
        assertThat(chamadoRepository.findByPrioridade(Prioridade.ALTA, pagina)
            .getTotalElements()).isEqualTo(2);
        assertThat(chamadoRepository.findByClienteId(ana.getId(), pagina)
            .getTotalElements()).isEqualTo(2);
        assertThat(chamadoRepository.findByStatusAndPrioridadeAndClienteId(
            StatusChamado.ABERTO, Prioridade.ALTA, ana.getId(), pagina)
            .getTotalElements()).isEqualTo(1);
    }

    @Test
    void devePaginarResultados() {
        Page<Chamado> pagina = chamadoRepository.findAll(PageRequest.of(0, 2));

        assertThat(pagina.getContent()).hasSize(2);
        assertThat(pagina.getTotalElements()).isEqualTo(3);
        assertThat(pagina.getTotalPages()).isEqualTo(2);
    }

    @Test
    void deveContarPorStatusEPrioridade() {
        assertThat(chamadoRepository.countByStatus(StatusChamado.ABERTO)).isEqualTo(2);
        assertThat(chamadoRepository.countByStatus(StatusChamado.FECHADO)).isZero();
        assertThat(chamadoRepository.countByPrioridade(Prioridade.ALTA)).isEqualTo(2);
    }

    @Test
    void deveIndicarRelacionamentosExistentes() {
        assertThat(chamadoRepository.existsByClienteId(ana.getId())).isTrue();
        assertThat(chamadoRepository.existsByCategoriaId(categoria.getId())).isTrue();
        assertThat(chamadoRepository.existsByTecnicoId(999L)).isFalse();
    }

    @Test
    void clienteRepositoryDeveDetectarEmailDuplicado() {
        assertThat(clienteRepository.existsByEmail("ana@exemplo.com")).isTrue();
        assertThat(clienteRepository.existsByEmail("outro@exemplo.com")).isFalse();
        assertThat(clienteRepository.existsByEmailAndIdNot("ana@exemplo.com", ana.getId()))
            .isFalse();
        assertThat(clienteRepository.existsByEmailAndIdNot("ana@exemplo.com", joao.getId()))
            .isTrue();
    }
}
