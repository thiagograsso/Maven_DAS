package br.com.sistemas.das.dto;

import br.com.sistemas.das.entity.StatusChamado;
import jakarta.validation.constraints.NotNull;

public record AlterarStatusRequest(
        @NotNull(message = "Status é obrigatório") StatusChamado status) { }
