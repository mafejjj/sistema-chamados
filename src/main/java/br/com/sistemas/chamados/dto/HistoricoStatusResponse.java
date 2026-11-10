package br.com.sistemas.chamados.dto;

import java.time.LocalDateTime;

import br.com.sistemas.chamados.entity.HistoricoStatus;

public record HistoricoStatusResponse(
    Long id,
    String statusAnterior,
    String statusNovo,
    LocalDateTime data
) {
    public static HistoricoStatusResponse de(HistoricoStatus historico) {
        return new HistoricoStatusResponse(
            historico.getId(),
            historico.getStatusAnterior() == null ? null : historico.getStatusAnterior().name(),
            historico.getStatusNovo().name(),
            historico.getData());
    }
}
