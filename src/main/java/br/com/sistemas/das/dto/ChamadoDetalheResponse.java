package br.com.sistemas.das.dto;

import java.time.LocalDateTime;
import java.util.List;

import br.com.sistemas.das.entity.Chamado;
import br.com.sistemas.das.entity.Comentario;
import br.com.sistemas.das.entity.Prioridade;
import br.com.sistemas.das.entity.StatusChamado;

public record ChamadoDetalheResponse(
        Long id,
        String titulo,
        String descricao,
        LocalDateTime dataAbertura,
        LocalDateTime dataFechamento,
        Prioridade prioridade,
        StatusChamado status,
        Long clienteId,
        Long categoriaId,
        Long tecnicoId,
        List<ComentarioResponse> comentarios) {
    public static ChamadoDetalheResponse de(Chamado chamado, List<Comentario> comentarios) {
        return new ChamadoDetalheResponse(
                chamado.getId(),
                chamado.getTitulo(),
                chamado.getDescricao(),
                chamado.getDataAbertura(),
                chamado.getDataFechamento(),
                chamado.getPrioridade(),
                chamado.getStatus(),
                chamado.getCliente().getId(),
                chamado.getCategoria().getId(),
                chamado.getTecnico().getId(),
                comentarios.stream().map(ComentarioResponse::de).toList());
    }
}
