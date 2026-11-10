package br.com.sistemas.chamados.service;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.sistemas.chamados.dto.DashboardResponse;
import br.com.sistemas.chamados.entity.Prioridade;
import br.com.sistemas.chamados.entity.StatusChamado;
import br.com.sistemas.chamados.repository.ChamadoRepository;

@Service
public class DashboardService {

    private final ChamadoRepository chamadoRepository;

    public DashboardService(ChamadoRepository chamadoRepository) {
        this.chamadoRepository = chamadoRepository;
    }

    @Transactional(readOnly = true)
    public DashboardResponse resumo() {
        Map<String, Long> porStatus = new LinkedHashMap<>();
        for (StatusChamado status : StatusChamado.values()) {
            porStatus.put(status.name(), chamadoRepository.countByStatus(status));
        }
        Map<String, Long> porPrioridade = new LinkedHashMap<>();
        for (Prioridade prioridade : Prioridade.values()) {
            porPrioridade.put(prioridade.name(), chamadoRepository.countByPrioridade(prioridade));
        }
        return new DashboardResponse(chamadoRepository.count(), porStatus, porPrioridade);
    }
}
