package br.com.sistemas.das.service;

import java.time.LocalDateTime;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import br.com.sistemas.das.dto.AtribuirTecnicoRequest;
import br.com.sistemas.das.dto.AlterarStatusRequest;
import br.com.sistemas.das.dto.ChamadoDetalheResponse;
import br.com.sistemas.das.dto.ChamadoRequest;
import br.com.sistemas.das.dto.ChamadoResponse;
import br.com.sistemas.das.dto.ComentarioRequest;
import br.com.sistemas.das.dto.ComentarioResponse;
import br.com.sistemas.das.entity.Chamado;
import br.com.sistemas.das.entity.Comentario;
import br.com.sistemas.das.entity.Prioridade;
import br.com.sistemas.das.entity.StatusChamado;
import br.com.sistemas.das.exception.RecursoNaoEncontradoException;
import br.com.sistemas.das.exception.RegraNegocioException;
import br.com.sistemas.das.repository.CategoriaRepository;
import br.com.sistemas.das.repository.ChamadoRepository;
import br.com.sistemas.das.repository.ChamadoSpecifications;
import br.com.sistemas.das.repository.ClienteRepository;
import br.com.sistemas.das.repository.ComentarioRepository;
import br.com.sistemas.das.repository.TecnicoRepository;

@Service
@Transactional
public class ChamadoService {
    private static final int TAMANHO_PAGINA_PADRAO = 20;
    private static final int TAMANHO_PAGINA_MAXIMO = 100;
    private static final Set<String> CAMPOS_ORDENACAO = Set.of(
            "id", "titulo", "dataAbertura", "dataFechamento", "prioridade", "status");

    private final ChamadoRepository chamadoRepository;
    private final ClienteRepository clienteRepository;
    private final CategoriaRepository categoriaRepository;
    private final TecnicoRepository tecnicoRepository;
    private final ComentarioRepository comentarioRepository;

    public ChamadoService(
            ChamadoRepository chamadoRepository,
            ClienteRepository clienteRepository,
            CategoriaRepository categoriaRepository,
            TecnicoRepository tecnicoRepository,
            ComentarioRepository comentarioRepository) {
        this.chamadoRepository = chamadoRepository;
        this.clienteRepository = clienteRepository;
        this.categoriaRepository = categoriaRepository;
        this.tecnicoRepository = tecnicoRepository;
        this.comentarioRepository = comentarioRepository;
    }

    @Transactional(readOnly = true)
    public Page<ChamadoResponse> listar(
            StatusChamado status, Prioridade prioridade, Long clienteId, Pageable pageable) {
        Pageable paginaValidada = validarPaginacao(pageable);
        return chamadoRepository.findAll(
                ChamadoSpecifications.comFiltros(status, prioridade, clienteId), paginaValidada)
                .map(ChamadoResponse::de);
    }

    @Transactional(readOnly = true)
    public Page<ChamadoResponse> listarPorCliente(Long clienteId, Pageable pageable) {
        if (!clienteRepository.existsById(clienteId)) {
            throw new RecursoNaoEncontradoException("Cliente não encontrado: " + clienteId);
        }
        return chamadoRepository.findByClienteId(clienteId, validarPaginacao(pageable))
                .map(ChamadoResponse::de);
    }

    @Transactional(readOnly = true)
    public ChamadoDetalheResponse buscar(Long id) {
        Chamado chamado = obter(id);
        return ChamadoDetalheResponse.de(chamado,
                comentarioRepository.findByChamadoIdOrderByDataCriacaoAscIdAsc(id));
    }

    public ChamadoResponse criar(ChamadoRequest request) {
        Chamado chamado = new Chamado();
        aplicar(chamado, request, true);
        return ChamadoResponse.de(chamadoRepository.save(chamado));
    }

    public ChamadoResponse atualizar(Long id, ChamadoRequest request) {
        Chamado chamado = obter(id);
        exigirChamadoAberto(chamado, "atualizar");
        aplicar(chamado, request, false);
        return ChamadoResponse.de(chamadoRepository.save(chamado));
    }

    public ChamadoResponse atribuir(Long id, AtribuirTecnicoRequest request) {
        Chamado chamado = obter(id);
        exigirChamadoAberto(chamado, "reatribuir técnico");
        chamado.setTecnico(tecnicoRepository.findById(request.tecnicoId())
                .orElseThrow(() -> new RecursoNaoEncontradoException(
                        "Técnico não encontrado: " + request.tecnicoId())));
        return ChamadoResponse.de(chamadoRepository.save(chamado));
    }

    public ChamadoResponse alterarStatus(Long id, AlterarStatusRequest request) {
        Chamado chamado = obter(id);
        alterarStatus(chamado, request.status());
        return ChamadoResponse.de(chamadoRepository.save(chamado));
    }

    public ComentarioResponse adicionarComentario(Long chamadoId, ComentarioRequest request) {
        Chamado chamado = obter(chamadoId);
        Comentario comentario = new Comentario(request.texto(), request.autor(), chamado);
        return ComentarioResponse.de(comentarioRepository.save(comentario));
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
        chamado.setPrioridade(request.prioridade());
        if (novo) {
            chamado.setStatus(request.status() == null ? StatusChamado.ABERTO : request.status());
            if (chamado.getStatus() == StatusChamado.FECHADO) {
                chamado.setDataFechamento(LocalDateTime.now());
            } else {
                chamado.setDataFechamento(null);
            }
        } else if (request.status() != null && request.status() != chamado.getStatus()) {
            alterarStatus(chamado, request.status());
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

    private void alterarStatus(Chamado chamado, StatusChamado novoStatus) {
        StatusChamado statusAtual = chamado.getStatus();
        if (statusAtual == novoStatus) {
            return;
        }
        boolean permitido = switch (statusAtual) {
            case ABERTO -> novoStatus == StatusChamado.EM_ATENDIMENTO;
            case EM_ATENDIMENTO -> novoStatus == StatusChamado.AGUARDANDO_CLIENTE
                    || novoStatus == StatusChamado.FECHADO;
            case AGUARDANDO_CLIENTE -> novoStatus == StatusChamado.EM_ATENDIMENTO
                    || novoStatus == StatusChamado.FECHADO;
            case FECHADO -> false;
        };
        if (!permitido) {
            throw new RegraNegocioException(
                    "Transição de status não permitida: " + statusAtual + " -> " + novoStatus);
        }
        chamado.setStatus(novoStatus);
        if (novoStatus == StatusChamado.FECHADO) {
            chamado.setDataFechamento(LocalDateTime.now());
        }
    }

    private void exigirChamadoAberto(Chamado chamado, String operacao) {
        if (chamado.getStatus() == StatusChamado.FECHADO) {
            throw new RegraNegocioException(
                    "Não é possível " + operacao + " um chamado fechado");
        }
    }

    private Pageable validarPaginacao(Pageable pageable) {
        if (pageable.getPageNumber() < 0
                || pageable.getPageSize() < 1
                || pageable.getPageSize() > TAMANHO_PAGINA_MAXIMO) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "A paginação deve usar page >= 0 e size entre 1 e " + TAMANHO_PAGINA_MAXIMO);
        }
        if (pageable.getSort().isUnsorted()) {
            return org.springframework.data.domain.PageRequest.of(
                    pageable.getPageNumber(), pageable.getPageSize(),
                    org.springframework.data.domain.Sort.by(
                            org.springframework.data.domain.Sort.Order.desc("dataAbertura")));
        }
        pageable.getSort().forEach(order -> {
            if (!CAMPOS_ORDENACAO.contains(order.getProperty())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Campo de ordenação não suportado: " + order.getProperty());
            }
        });
        return pageable;
    }

    private Chamado obter(Long id) {
        return chamadoRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Chamado não encontrado: " + id));
    }
}
