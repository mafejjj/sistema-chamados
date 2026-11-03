package br.com.sistemas.chamados.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.sistemas.chamados.dto.AlterarStatusRequest;
import br.com.sistemas.chamados.dto.AtribuirTecnicoRequest;
import br.com.sistemas.chamados.dto.ChamadoRequest;
import br.com.sistemas.chamados.dto.ChamadoResponse;
import br.com.sistemas.chamados.dto.ComentarioResponse;
import br.com.sistemas.chamados.entity.Categoria;
import br.com.sistemas.chamados.entity.Chamado;
import br.com.sistemas.chamados.entity.Cliente;
import br.com.sistemas.chamados.entity.StatusChamado;
import br.com.sistemas.chamados.entity.Tecnico;
import br.com.sistemas.chamados.exception.RecursoNaoEncontradoException;
import br.com.sistemas.chamados.exception.RegraNegocioException;
import br.com.sistemas.chamados.repository.CategoriaRepository;
import br.com.sistemas.chamados.repository.ChamadoRepository;
import br.com.sistemas.chamados.repository.ClienteRepository;
import br.com.sistemas.chamados.repository.ComentarioRepository;
import br.com.sistemas.chamados.repository.TecnicoRepository;

@Service
public class ChamadoService {

    private final ChamadoRepository repository;
    private final ClienteRepository clienteRepository;
    private final CategoriaRepository categoriaRepository;
    private final TecnicoRepository tecnicoRepository;
    private final ComentarioRepository comentarioRepository;

    public ChamadoService(ChamadoRepository repository,
        ClienteRepository clienteRepository, CategoriaRepository categoriaRepository,
        TecnicoRepository tecnicoRepository, ComentarioRepository comentarioRepository) {
        this.repository = repository;
        this.clienteRepository = clienteRepository;
        this.categoriaRepository = categoriaRepository;
        this.tecnicoRepository = tecnicoRepository;
        this.comentarioRepository = comentarioRepository;
    }

    @Transactional
    public ChamadoResponse criar(ChamadoRequest dto) {
        Cliente cliente = buscarCliente(dto.clienteId());
        Categoria categoria = buscarCategoria(dto.categoriaId());
        Chamado chamado = new Chamado(
            dto.titulo(), dto.descricao(), dto.prioridade(), cliente, categoria);
        return ChamadoResponse.de(repository.save(chamado));
    }

    @Transactional(readOnly = true)
    public Page<ChamadoResponse> listar(StatusChamado status, br.com.sistemas.chamados.entity.Prioridade prioridade,
        Long clienteId, Pageable pageable) {
        Page<Chamado> chamados;
        if (status != null && prioridade != null && clienteId != null) {
            chamados = repository.findByStatusAndPrioridadeAndClienteId(
                status, prioridade, clienteId, pageable);
        } else if (status != null && prioridade != null) {
            chamados = repository.findByStatusAndPrioridade(status, prioridade, pageable);
        } else if (status != null && clienteId != null) {
            chamados = repository.findByStatusAndClienteId(status, clienteId, pageable);
        } else if (prioridade != null && clienteId != null) {
            chamados = repository.findByPrioridadeAndClienteId(prioridade, clienteId, pageable);
        } else if (status != null) {
            chamados = repository.findByStatus(status, pageable);
        } else if (prioridade != null) {
            chamados = repository.findByPrioridade(prioridade, pageable);
        } else if (clienteId != null) {
            chamados = repository.findByClienteId(clienteId, pageable);
        } else {
            chamados = repository.findAll(pageable);
        }
        return chamados.map(ChamadoResponse::de);
    }

    @Transactional(readOnly = true)
    public Page<ChamadoResponse> listarPorCliente(Long clienteId, Pageable pageable) {
        buscarCliente(clienteId);
        return repository.findByClienteId(clienteId, pageable).map(ChamadoResponse::de);
    }

    @Transactional(readOnly = true)
    public ChamadoResponse buscar(Long id) {
        Chamado chamado = buscarEntity(id);
        List<ComentarioResponse> comentarios = comentarioRepository
            .findByChamadoIdOrderByDataAsc(id)
            .stream().map(ComentarioResponse::de).toList();
        return ChamadoResponse.de(chamado, comentarios);
    }

    @Transactional
    public ChamadoResponse atualizar(Long id, ChamadoRequest dto) {
        Chamado chamado = buscarEntity(id);
        impedirAlteracaoSeFechado(chamado);
        chamado.setTitulo(dto.titulo());
        chamado.setDescricao(dto.descricao());
        chamado.setPrioridade(dto.prioridade());
        chamado.setCliente(buscarCliente(dto.clienteId()));
        chamado.setCategoria(buscarCategoria(dto.categoriaId()));
        return ChamadoResponse.de(chamado);
    }

    @Transactional
    public void excluir(Long id) {
        Chamado chamado = buscarEntity(id);
        impedirAlteracaoSeFechado(chamado);
        repository.delete(chamado);
    }

    @Transactional
    public ChamadoResponse atribuir(Long id, AtribuirTecnicoRequest dto) {
        Chamado chamado = buscarEntity(id);
        impedirAlteracaoSeFechado(chamado);
        Tecnico tecnico = tecnicoRepository.findById(dto.tecnicoId())
            .orElseThrow(() -> new RecursoNaoEncontradoException(
                "Técnico " + dto.tecnicoId() + " não encontrado"));
        chamado.setTecnico(tecnico);
        return ChamadoResponse.de(chamado);
    }

    @Transactional
    public ChamadoResponse alterarStatus(Long id, AlterarStatusRequest dto) {
        Chamado chamado = buscarEntity(id);
        impedirAlteracaoSeFechado(chamado);
        validarTransicao(chamado.getStatus(), dto.status());
        chamado.setStatus(dto.status());
        chamado.setDataFechamento(dto.status() == StatusChamado.FECHADO
            ? LocalDateTime.now() : null);
        return ChamadoResponse.de(chamado);
    }

    private void validarTransicao(StatusChamado atual, StatusChamado novo) {
        boolean valida = switch (atual) {
            case ABERTO -> novo == StatusChamado.EM_ATENDIMENTO;
            case EM_ATENDIMENTO -> novo == StatusChamado.AGUARDANDO_CLIENTE
                || novo == StatusChamado.FECHADO;
            case AGUARDANDO_CLIENTE -> novo == StatusChamado.EM_ATENDIMENTO
                || novo == StatusChamado.FECHADO;
            case FECHADO -> false;
        };
        if (!valida) {
            throw new RegraNegocioException(
                "Transição de status inválida: " + atual + " para " + novo);
        }
    }

    private void impedirAlteracaoSeFechado(Chamado chamado) {
        if (chamado.getStatus() == StatusChamado.FECHADO) {
            throw new RegraNegocioException("Chamado fechado não pode ser alterado");
        }
    }

    private Chamado buscarEntity(Long id) {
        return repository.findById(id)
            .orElseThrow(() -> new RecursoNaoEncontradoException(
                "Chamado " + id + " não encontrado"));
    }

    private Cliente buscarCliente(Long id) {
        return clienteRepository.findById(id)
            .orElseThrow(() -> new RecursoNaoEncontradoException(
                "Cliente " + id + " não encontrado"));
    }

    private Categoria buscarCategoria(Long id) {
        return categoriaRepository.findById(id)
            .orElseThrow(() -> new RecursoNaoEncontradoException(
                "Categoria " + id + " não encontrada"));
    }
}