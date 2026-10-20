package br.com.sistemas.das.dto;

import br.com.sistemas.das.entity.Cliente;
import br.com.sistemas.chamados.entity.Cliente;

public record ClientResponse(Long id, String nome, String email, String telephone) {
    public static ClientResponse de(Cliente c) {
        return new ClientResponse(c.getId(), c.getNome(), c.getEmail(), c.getTelephone());
    }
}