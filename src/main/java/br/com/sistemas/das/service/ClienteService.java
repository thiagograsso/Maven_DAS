package br.com.sistemas.das.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.sistemas.das.dto.ClienteRequest;
import br.com.sistemas.das.dto.ClienteResponse;
import br.com.sistemas.das.entity.Cliente;
import br.com.sistemas.das.exception.RecursoNaoEncontradoException;
import br.com.sistemas.das.exception.RegraNegocioException;
import br.com.sistemas.das.repository.ClienteRepository;

@Service
@Transactional
public class ClienteService {
    private final ClienteRepository repository;

    public ClienteService(ClienteRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<ClienteResponse> listar() {
        return repository.findAll().stream().map(ClienteResponse::de).toList();
    }

    @Transactional(readOnly = true)
    public ClienteResponse buscar(Long id) {
        return ClienteResponse.de(obter(id));
    }

    public ClienteResponse criar(ClienteRequest request) {
        validarEmailDuplicado(request.email(), null);
        return ClienteResponse.de(repository.save(
                new Cliente(request.nome(), request.email(), request.telefone())));
    }

    public ClienteResponse atualizar(Long id, ClienteRequest request) {
        Cliente cliente = obter(id);
        validarEmailDuplicado(request.email(), id);
        cliente.setNome(request.nome());
        cliente.setEmail(request.email());
        cliente.setTelefone(request.telefone());
        return ClienteResponse.de(repository.save(cliente));
    }

    public void excluir(Long id) {
        repository.delete(obter(id));
    }

    private Cliente obter(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Cliente não encontrado: " + id));
    }

    private void validarEmailDuplicado(String email, Long id) {
        boolean duplicado = id == null
                ? repository.existsByEmail(email)
                : repository.existsByEmailAndIdNot(email, id);
        if (duplicado) {
            throw new RegraNegocioException("Já existe um cliente com esse e-mail");
        }
    }
}
