package br.com.sistemas.das.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.sistemas.das.dto.ChamadoRequest;
import br.com.sistemas.das.dto.ChamadoResponse;
import br.com.sistemas.das.entity.Chamado;
import br.com.sistemas.das.entity.StatusChamado;
import br.com.sistemas.das.exception.RecursoNaoEncontradoException;
import br.com.sistemas.das.repository.CategoriaRepository;
import br.com.sistemas.das.repository.ChamadoRepository;
import br.com.sistemas.das.repository.ClienteRepository;
import br.com.sistemas.das.repository.TecnicoRepository;

@Service
@Transactional
public class ChamadoService {
    private final ChamadoRepository chamadoRepository;
    private final ClienteRepository clienteRepository;
    private final CategoriaRepository categoriaRepository;
    private final TecnicoRepository tecnicoRepository;

    public ChamadoService(
            ChamadoRepository chamadoRepository,
            ClienteRepository clienteRepository,
            CategoriaRepository categoriaRepository,
            TecnicoRepository tecnicoRepository) {
        this.chamadoRepository = chamadoRepository;
        this.clienteRepository = clienteRepository;
        this.categoriaRepository = categoriaRepository;
        this.tecnicoRepository = tecnicoRepository;
    }

    @Transactional(readOnly = true)
    public List<ChamadoResponse> listar() {
        return chamadoRepository.findAll().stream().map(ChamadoResponse::de).toList();
    }

    @Transactional(readOnly = true)
    public ChamadoResponse buscar(Long id) {
        return ChamadoResponse.de(obter(id));
    }

    public ChamadoResponse criar(ChamadoRequest request) {
        Chamado chamado = new Chamado();
        aplicar(chamado, request, true);
        return ChamadoResponse.de(chamadoRepository.save(chamado));
    }

    public ChamadoResponse atualizar(Long id, ChamadoRequest request) {
        Chamado chamado = obter(id);
        aplicar(chamado, request, false);
        return ChamadoResponse.de(chamadoRepository.save(chamado));
    }

    public void excluir(Long id) {
        chamadoRepository.delete(obter(id));
    }

    private void aplicar(Chamado chamado, ChamadoRequest request, boolean novo) {
        chamado.setTitulo(request.titulo());
        chamado.setDescricao(request.descricao());
        if (novo || request.dataAbertura() != null) {
            chamado.setDataAbertura(request.dataAbertura());
        }
        chamado.setDataFechamento(request.dataFechamento());
        chamado.setPrioridade(request.prioridade());
        if (request.status() != null) {
            chamado.setStatus(request.status());
        } else if (novo) {
            chamado.setStatus(StatusChamado.ABERTO);
        }
        chamado.setCliente(clienteRepository.findById(request.clienteId())
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Cliente não encontrado: " + request.clienteId())));
        chamado.setCategoria(categoriaRepository.findById(request.categoriaId())
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Categoria não encontrada: " + request.categoriaId())));
        chamado.setTecnico(tecnicoRepository.findById(request.tecnicoId())
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Técnico não encontrado: " + request.tecnicoId())));
    }

    private Chamado obter(Long id) {
        return chamadoRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Chamado não encontrado: " + id));
    }
}
