package br.com.sistemas.chamados.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import br.com.sistemas.chamados.entity.Chamado;
import br.com.sistemas.chamados.entity.Prioridade;
import br.com.sistemas.chamados.entity.StatusChamado;

public interface ChamadoRepository extends JpaRepository<Chamado, Long> {
    boolean existsByClienteId(Long clienteId);
    boolean existsByCategoriaId(Long categoriaId);
    boolean existsByTecnicoId(Long tecnicoId);

    Page<Chamado> findByStatusAndPrioridadeAndClienteId(
        StatusChamado status, Prioridade prioridade, Long clienteId, Pageable pageable);

    Page<Chamado> findByStatusAndPrioridade(
        StatusChamado status, Prioridade prioridade, Pageable pageable);

    Page<Chamado> findByStatusAndClienteId(
        StatusChamado status, Long clienteId, Pageable pageable);

    Page<Chamado> findByPrioridadeAndClienteId(
        Prioridade prioridade, Long clienteId, Pageable pageable);

    Page<Chamado> findByStatus(StatusChamado status, Pageable pageable);

    Page<Chamado> findByPrioridade(Prioridade prioridade, Pageable pageable);

    Page<Chamado> findByClienteId(Long clienteId, Pageable pageable);

    long countByStatus(StatusChamado status);

    long countByPrioridade(Prioridade prioridade);
}