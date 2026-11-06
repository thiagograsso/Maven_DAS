package br.com.sistemas.das.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ComentarioRequest(
        @NotBlank(message = "Texto do comentário é obrigatório")
        @Size(max = 2000, message = "Comentário deve ter no máximo 2000 caracteres")
        String texto,

        @NotBlank(message = "Autor do comentário é obrigatório")
        @Size(max = 100, message = "Autor deve ter no máximo 100 caracteres")
        String autor) { }
