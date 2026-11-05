package br.com.sistemas.das.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import br.com.sistemas.das.dto.AlterarStatusRequest;
import br.com.sistemas.das.dto.ChamadoRequest;
import br.com.sistemas.das.entity.Categoria;
import br.com.sistemas.das.entity.Chamado;
import br.com.sistemas.das.entity.Cliente;
import br.com.sistemas.das.entity.Perfil;
import br.com.sistemas.das.entity.Prioridade;
import br.com.sistemas.das.entity.StatusChamado;
import br.com.sistemas.das.entity.Tecnico;
import br.com.sistemas.das.exception.RegraNegocioException;
import br.com.sistemas.das.repository.CategoriaRepository;
import br.com.sistemas.das.repository.ChamadoRepository;
import br.com.sistemas.das.repository.ClienteRepository;
import br.com.sistemas.das.repository.ComentarioRepository;
import br.com.sistemas.das.repository.TecnicoRepository;

class ChamadoServiceTests {
    private final ChamadoRepository chamadoRepository = mock(ChamadoRepository.class);
    private final ClienteRepository clienteRepository = mock(ClienteRepository.class);
    private final CategoriaRepository categoriaRepository = mock(CategoriaRepository.class);
    private final TecnicoRepository tecnicoRepository = mock(TecnicoRepository.class);
    private final ComentarioRepository comentarioRepository = mock(ComentarioRepository.class);
    private final ChamadoService service = new ChamadoService(
            chamadoRepository, clienteRepository, categoriaRepository, tecnicoRepository, comentarioRepository);

    private Chamado chamado;

    @BeforeEach
    void prepararChamado() {
        Cliente cliente = new Cliente("Cliente", "cliente@example.com", null);
        Categoria categoria = new Categoria("Rede", "Conectividade");
        Tecnico tecnico = new Tecnico("Ana", "ana@example.com", Perfil.TECNICO);
        chamado = new Chamado();
        chamado.setTitulo("Sem conexão");
        chamado.setDescricao("Falha na rede");
        chamado.setPrioridade(Prioridade.ALTA);
        chamado.setStatus(StatusChamado.ABERTO);
        chamado.setCliente(cliente);
        chamado.setCategoria(categoria);
        chamado.setTecnico(tecnico);
        when(chamadoRepository.findById(1L)).thenReturn(Optional.of(chamado));
        when(chamadoRepository.save(chamado)).thenReturn(chamado);
    }

    @Test
    void permiteSomenteAsTransicoesConservadorasERegistraFechamento() {
        service.alterarStatus(1L, new AlterarStatusRequest(StatusChamado.EM_ATENDIMENTO));
        assertThat(chamado.getStatus()).isEqualTo(StatusChamado.EM_ATENDIMENTO);

        service.alterarStatus(1L, new AlterarStatusRequest(StatusChamado.AGUARDANDO_CLIENTE));
        assertThat(chamado.getStatus()).isEqualTo(StatusChamado.AGUARDANDO_CLIENTE);

        service.alterarStatus(1L, new AlterarStatusRequest(StatusChamado.FECHADO));

        assertThat(chamado.getStatus()).isEqualTo(StatusChamado.FECHADO);
        assertThat(chamado.getDataFechamento()).isNotNull();
    }

    @Test
    void rejeitaTransicaoInvalidaSemAlterarEstado() {
        assertThatThrownBy(() ->
                service.alterarStatus(1L, new AlterarStatusRequest(StatusChamado.FECHADO)))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("ABERTO -> FECHADO");
        assertThat(chamado.getStatus()).isEqualTo(StatusChamado.ABERTO);
        assertThat(chamado.getDataFechamento()).isNull();
    }

    @Test
    void rejeitaReatribuicaoEAtualizacaoDeChamadoFechado() {
        chamado.setStatus(StatusChamado.FECHADO);

        assertThatThrownBy(() -> service.atribuir(1L, new br.com.sistemas.das.dto.AtribuirTecnicoRequest(2L)))
                .isInstanceOf(RegraNegocioException.class);
        assertThatThrownBy(() -> service.atualizar(1L, (ChamadoRequest) null))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("chamado fechado");
    }

    @Test
    void atribuirTecnicoNaoFechaNemAlteraStatusDeChamadoAberto() {
        when(tecnicoRepository.findById(2L))
                .thenReturn(Optional.of(new Tecnico("Carlos", "carlos@example.com", Perfil.TECNICO)));

        var response = service.atribuir(1L, new br.com.sistemas.das.dto.AtribuirTecnicoRequest(2L));

        assertThat(response.status()).isEqualTo(StatusChamado.ABERTO);
        assertThat(chamado.getTecnico().getNome()).isEqualTo("Carlos");
        assertThat(chamado.getStatus()).isEqualTo(StatusChamado.ABERTO);
    }
}
