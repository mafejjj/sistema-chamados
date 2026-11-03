package br.com.sistemas.chamados.dto;

import java.time.LocalDateTime;
import java.util.List;

import br.com.sistemas.chamados.entity.Chamado;

public record ChamadoResponse(
    Long id,
    String titulo,
    String descricao,
    LocalDateTime dataAbertura,
    LocalDateTime dataFechamento,
    String prioridade,
    String status,
    Long clienteId,
    Long categoriaId,
    Long tecnicoId,
    List<ComentarioResponse> comentarios
) {
    public static ChamadoResponse de(Chamado chamado) {
        return de(chamado, List.of());
    }

    public static ChamadoResponse de(Chamado chamado, List<ComentarioResponse> comentarios) {
        return new ChamadoResponse(
            chamado.getId(), chamado.getTitulo(), chamado.getDescricao(),
            chamado.getDataAbertura(), chamado.getDataFechamento(),
            chamado.getPrioridade().name(), chamado.getStatus().name(),
            chamado.getCliente().getId(), chamado.getCategoria().getId(),
            chamado.getTecnico() == null ? null : chamado.getTecnico().getId(), comentarios);
    }
}