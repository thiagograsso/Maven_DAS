package br.com.sistemas.das.dto;

import br.com.sistemas.das.entity.Categoria;

public record CategoriaResponse(Long id, String nome, String descricao) {
    public static CategoriaResponse de(Categoria categoria) {
        return new CategoriaResponse(categoria.getId(), categoria.getNome(), categoria.getDescricao());
    }
}
