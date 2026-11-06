package br.com.sistemas.das.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.sistemas.das.dto.CategoriaRequest;
import br.com.sistemas.das.dto.CategoriaResponse;
import br.com.sistemas.das.entity.Categoria;
import br.com.sistemas.das.exception.RecursoNaoEncontradoException;
import br.com.sistemas.das.exception.RegraNegocioException;
import br.com.sistemas.das.repository.CategoriaRepository;

@Service
@Transactional
public class CategoriaService {
    private final CategoriaRepository repository;

    public CategoriaService(CategoriaRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<CategoriaResponse> listar() {
        return repository.findAll().stream().map(CategoriaResponse::de).toList();
    }

    @Transactional(readOnly = true)
    public CategoriaResponse buscar(Long id) {
        return CategoriaResponse.de(obter(id));
    }

    public CategoriaResponse criar(CategoriaRequest request) {
        validarNomeDuplicado(request.nome(), null);
        return CategoriaResponse.de(repository.save(new Categoria(request.nome(), request.descricao())));
    }

    public CategoriaResponse atualizar(Long id, CategoriaRequest request) {
        Categoria categoria = obter(id);
        validarNomeDuplicado(request.nome(), id);
        categoria.setNome(request.nome());
        categoria.setDescricao(request.descricao());
        return CategoriaResponse.de(repository.save(categoria));
    }

    public void excluir(Long id) {
        repository.delete(obter(id));
    }

    private Categoria obter(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Categoria não encontrada: " + id));
    }

    private void validarNomeDuplicado(String nome, Long id) {
        boolean duplicado = id == null
                ? repository.existsByNomeIgnoreCase(nome)
                : repository.existsByNomeIgnoreCaseAndIdNot(nome, id);
        if (duplicado) {
            throw new RegraNegocioException("Já existe uma categoria com esse nome");
        }
    }
}
