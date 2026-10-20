package br.com.sistemas.das.dto;

import br.com.sistemas.das.entity.Cliente;

public record ClienteResponse(Long id, String nome, String email, String telefone) {
    public static ClienteResponse de(Cliente c) {
        return new ClienteResponse(c.getId(), c.getNome(), c.getEmail(), c.getTelefone());
    }
}