package br.com.sistemas.chamados.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import br.com.sistemas.chamados.entity.HistoricoStatus;

public interface HistoricoStatusRepository extends JpaRepository<HistoricoStatus, Long> {
    List<HistoricoStatus> findByChamadoIdOrderByDataAscIdAsc(Long chamadoId);
    void deleteByChamadoId(Long chamadoId);
}
