package br.com.sistemas.das.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.sistemas.das.dto.TecnicoRequest;
import br.com.sistemas.das.dto.TecnicoResponse;
import br.com.sistemas.das.entity.Tecnico;
import br.com.sistemas.das.exception.RecursoNaoEncontradoException;
import br.com.sistemas.das.exception.RegraNegocioException;
import br.com.sistemas.das.repository.TecnicoRepository;

@Service
@Transactional
public class TecnicoService {
    private final TecnicoRepository repository;

    public TecnicoService(TecnicoRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<TecnicoResponse> listar() {
        return repository.findAll().stream().map(TecnicoResponse::de).toList();
    }

    @Transactional(readOnly = true)
    public TecnicoResponse buscar(Long id) {
        return TecnicoResponse.de(obter(id));
    }

    public TecnicoResponse criar(TecnicoRequest request) {
        validarEmailDuplicado(request.email(), null);
        return TecnicoResponse.de(repository.save(
                new Tecnico(request.nome(), request.email(), request.perfil())));
    }

    public TecnicoResponse atualizar(Long id, TecnicoRequest request) {
        Tecnico tecnico = obter(id);
        validarEmailDuplicado(request.email(), id);
        tecnico.setNome(request.nome());
        tecnico.setEmail(request.email());
        tecnico.setPerfil(request.perfil());
        return TecnicoResponse.de(repository.save(tecnico));
    }

    public void excluir(Long id) {
        repository.delete(obter(id));
    }

    private Tecnico obter(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Técnico não encontrado: " + id));
    }

    private void validarEmailDuplicado(String email, Long id) {
        boolean duplicado = id == null
                ? repository.existsByEmail(email)
                : repository.existsByEmailAndIdNot(email, id);
        if (duplicado) {
            throw new RegraNegocioException("Já existe um técnico com esse e-mail");
        }
    }
}
