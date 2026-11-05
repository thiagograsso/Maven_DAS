package br.com.sistemas.das.dto;

import java.time.LocalDateTime;

import br.com.sistemas.das.entity.Chamado;
import br.com.sistemas.das.entity.Prioridade;
import br.com.sistemas.das.entity.StatusChamado;

public record ChamadoResponse(
    Long id,
    String titulo,
    String descricao,
    LocalDateTime dataAbertura,
    LocalDateTime dataFechamento,
    Prioridade prioridade,
    StatusChamado status,
    Long clienteId,
    Long categoriaId,
    Long tecnicoId
) {
    public static ChamadoResponse de(Chamado chamado) {
        return new ChamadoResponse(chamado.getId(), chamado.getTitulo(), chamado.getDescricao(),
                chamado.getDataAbertura(), chamado.getDataFechamento(), chamado.getPrioridade(),
                chamado.getStatus(), chamado.getCliente().getId(), chamado.getCategoria().getId(),
                chamado.getTecnico().getId());
    }
}
