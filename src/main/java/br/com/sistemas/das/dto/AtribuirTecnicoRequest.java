package br.com.sistemas.das.dto;

import jakarta.validation.constraints.NotNull;

public record AtribuirTecnicoRequest(
        @NotNull(message = "Técnico é obrigatório") Long tecnicoId) { }
