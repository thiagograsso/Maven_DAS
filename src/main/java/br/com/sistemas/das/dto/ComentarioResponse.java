package br.com.sistemas.das.dto;

import java.time.LocalDateTime;

import br.com.sistemas.das.entity.Comentario;

public record ComentarioResponse(
        Long id, String texto, String autor, LocalDateTime dataCriacao) {
    public static ComentarioResponse de(Comentario comentario) {
        return new ComentarioResponse(comentario.getId(), comentario.getTexto(),
                comentario.getAutor(), comentario.getDataCriacao());
    }
}
