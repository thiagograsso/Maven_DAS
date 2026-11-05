package br.com.sistemas.das.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import br.com.sistemas.das.entity.Categoria;
import br.com.sistemas.das.entity.Chamado;
import br.com.sistemas.das.entity.Cliente;
import br.com.sistemas.das.entity.Comentario;
import br.com.sistemas.das.entity.Perfil;
import br.com.sistemas.das.entity.Prioridade;
import br.com.sistemas.das.entity.StatusChamado;
import br.com.sistemas.das.entity.Tecnico;

@DataJpaTest
class ChamadoRepositoryTests {
    @Autowired
    private ChamadoRepository chamadoRepository;

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private CategoriaRepository categoriaRepository;

    @Autowired
    private TecnicoRepository tecnicoRepository;

    @Autowired
    private ComentarioRepository comentarioRepository;

    private Cliente clienteUm;
    private Cliente clienteDois;
    private Categoria categoria;
    private Tecnico tecnico;

    @BeforeEach
    void prepararDados() {
        clienteUm = clienteRepository.save(new Cliente("Cliente Um", "um@example.com", null));
        clienteDois = clienteRepository.save(new Cliente("Cliente Dois", "dois@example.com", null));
        categoria = categoriaRepository.save(new Categoria("Rede", "Conectividade"));
        tecnico = tecnicoRepository.save(new Tecnico("Ana", "ana@example.com", Perfil.TECNICO));
        salvarChamado("A", StatusChamado.ABERTO, Prioridade.ALTA, clienteUm, LocalDateTime.parse("2026-01-03T10:00:00"));
        salvarChamado("B", StatusChamado.EM_ATENDIMENTO, Prioridade.MEDIA, clienteUm, LocalDateTime.parse("2026-01-02T10:00:00"));
        salvarChamado("C", StatusChamado.ABERTO, Prioridade.ALTA, clienteDois, LocalDateTime.parse("2026-01-01T10:00:00"));
    }

    @Test
    void aplicaFiltrosOpcionaisEmCombinacaoEOrdenaComPaginacao() {
        Page<Chamado> resultado = chamadoRepository.findAll(
                ChamadoSpecifications.comFiltros(
                        StatusChamado.ABERTO, Prioridade.ALTA, clienteUm.getId()),
                PageRequest.of(0, 1, Sort.by(Sort.Direction.DESC, "dataAbertura")));

        assertThat(resultado.getTotalElements()).isEqualTo(1);
        assertThat(resultado.getContent()).extracting(Chamado::getTitulo).containsExactly("A");
        assertThat(resultado.getTotalPages()).isEqualTo(1);
    }

    @Test
    void listaChamadosDoClienteComPaginacao() {
        Page<Chamado> resultado = chamadoRepository.findByClienteId(
                clienteUm.getId(), PageRequest.of(0, 1, Sort.by(Sort.Direction.DESC, "dataAbertura")));

        assertThat(resultado.getTotalElements()).isEqualTo(2);
        assertThat(resultado.getContent()).extracting(Chamado::getTitulo).containsExactly("A");
        assertThat(resultado.getTotalPages()).isEqualTo(2);
    }

    @Test
    void persisteComentarioComAssociacaoAoChamado() {
        Chamado chamado = chamadoRepository.findAll().stream()
                .filter(item -> item.getTitulo().equals("A"))
                .findFirst().orElseThrow();
        Comentario comentario = comentarioRepository.save(
                new Comentario("Resolvido com o cliente.", "Ana", chamado));

        List<Comentario> carregados =
                comentarioRepository.findByChamadoIdOrderByDataCriacaoAscIdAsc(chamado.getId());

        assertThat(carregados).containsExactly(comentario);
        assertThat(carregados.getFirst().getTexto()).isEqualTo("Resolvido com o cliente.");
        assertThat(carregados.getFirst().getAutor()).isEqualTo("Ana");
        assertThat(carregados.getFirst().getDataCriacao()).isNotNull();
        assertThat(carregados.getFirst().getChamado().getId()).isEqualTo(chamado.getId());
    }

    private void salvarChamado(
            String titulo, StatusChamado status, Prioridade prioridade, Cliente cliente, LocalDateTime abertura) {
        Chamado chamado = new Chamado();
        chamado.setTitulo(titulo);
        chamado.setDescricao("Descrição de " + titulo);
        chamado.setDataAbertura(abertura);
        chamado.setPrioridade(prioridade);
        chamado.setStatus(status);
        chamado.setCliente(cliente);
        chamado.setCategoria(categoria);
        chamado.setTecnico(tecnico);
        chamadoRepository.save(chamado);
    }
}
