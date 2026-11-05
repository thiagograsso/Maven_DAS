package br.com.sistemas.das.dto;

import br.com.sistemas.das.entity.Tecnico;
import br.com.sistemas.das.entity.Perfil;

public record TecnicoResponse(Long id, String nome, String email, Perfil perfil) {
    public static TecnicoResponse de(Tecnico tecnico) {
        return new TecnicoResponse(tecnico.getId(), tecnico.getNome(), tecnico.getEmail(), tecnico.getPerfil());
    }
}
