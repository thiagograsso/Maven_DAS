package br.com.sistemas.das.dto;

import java.time.LocalDateTime;

import br.com.sistemas.das.entity.Prioridade;
import br.com.sistemas.das.entity.StatusChamado;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ChamadoRequest(
    @NotBlank(message = "Título é obrigatório")
    @Size(max = 150, message = "Título deve ter no máximo 150 caracteres")
    String titulo,

    @NotBlank(message = "Descrição é obrigatória")
    @Size(max = 2000, message = "Descrição deve ter no máximo 2000 caracteres")
    String descricao,

    LocalDateTime dataAbertura,
    LocalDateTime dataFechamento,

    @NotNull(message = "Prioridade é obrigatória")
    Prioridade prioridade,

    StatusChamado status,

    @NotNull(message = "Cliente é obrigatório")
    Long clienteId,

    @NotNull(message = "Categoria é obrigatória")
    Long categoriaId,

    @NotNull(message = "Técnico é obrigatório")
    Long tecnicoId
) { }
