package br.com.sistemas.das.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import br.com.sistemas.das.dto.ChamadoDetalheResponse;
import br.com.sistemas.das.dto.ChamadoResponse;
import br.com.sistemas.das.dto.ComentarioResponse;
import br.com.sistemas.das.entity.Prioridade;
import br.com.sistemas.das.entity.StatusChamado;
import br.com.sistemas.das.exception.GlobalExceptionHandler;
import br.com.sistemas.das.exception.RecursoNaoEncontradoException;
import br.com.sistemas.das.service.ChamadoService;

@WebMvcTest(controllers = {ChamadoController.class, ClienteChamadoController.class})
@Import(GlobalExceptionHandler.class)
class ChamadoControllerTests {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ChamadoService service;

    @Test
    void encaminhaFiltrosPaginacaoESortDaListagem() throws Exception {
        ChamadoResponse chamado = resposta();
        var page = new PageImpl<>(List.of(chamado),
                PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "dataAbertura")), 1);
        when(service.listar(eq(StatusChamado.ABERTO), eq(Prioridade.ALTA), eq(42L), any()))
                .thenReturn(page);

        mockMvc.perform(get("/chamados")
                        .queryParam("status", "ABERTO")
                        .queryParam("prioridade", "ALTA")
                        .queryParam("clienteId", "42")
                        .queryParam("page", "0")
                        .queryParam("size", "20")
                        .queryParam("sort", "dataAbertura,desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(7))
                .andExpect(jsonPath("$.totalElements").value(1));

        verify(service).listar(eq(StatusChamado.ABERTO), eq(Prioridade.ALTA), eq(42L), any());
    }

    @Test
    void listaChamadosDoClienteNaRotaPublica() throws Exception {
        when(service.listarPorCliente(eq(42L), any()))
                .thenReturn(new PageImpl<>(List.of(resposta())));

        mockMvc.perform(get("/clientes/42/chamados"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].clienteId").value(42));

        verify(service).listarPorCliente(eq(42L), any());
    }

    @Test
    void encaminhaAcoesDeStatusEAtribuicao() throws Exception {
        when(service.alterarStatus(eq(7L), any())).thenReturn(resposta());
        when(service.atribuir(eq(7L), any())).thenReturn(resposta());

        mockMvc.perform(patch("/chamados/7/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"EM_ATENDIMENTO\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ABERTO"));
        mockMvc.perform(patch("/chamados/7/atribuir")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tecnicoId\":9}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(7));
    }

    @Test
    void validaCriacaoEComentarioEDevolveDetalheComHistorico() throws Exception {
        mockMvc.perform(post("/chamados")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"titulo\":\"\",\"descricao\":\"\",\"prioridade\":null}"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/chamados/7/comentarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"texto\":\"  \",\"autor\":\"Ana\"}"))
                .andExpect(status().isBadRequest());

        when(service.adicionarComentario(eq(7L), any()))
                .thenReturn(new ComentarioResponse(2L, "Resolvido", "Ana", LocalDateTime.now()));
        mockMvc.perform(post("/chamados/7/comentarios")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"texto\":\"Resolvido\",\"autor\":\"Ana\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.autor").value("Ana"));

        when(service.buscar(7L)).thenReturn(new ChamadoDetalheResponse(
                7L, "Rede", "Sem conexão", LocalDateTime.now(), null, Prioridade.ALTA,
                StatusChamado.ABERTO, 42L, 3L, 9L,
                List.of(new ComentarioResponse(2L, "Resolvido", "Ana", LocalDateTime.now()))));
        mockMvc.perform(get("/chamados/7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.comentarios[0].texto").value("Resolvido"));
    }

    @Test
    void retorna404ParaChamadoInexistente() throws Exception {
        when(service.buscar(99L)).thenThrow(new RecursoNaoEncontradoException("Chamado não encontrado: 99"));

        mockMvc.perform(get("/chamados/99"))
                .andExpect(status().isNotFound());
    }

    private ChamadoResponse resposta() {
        return new ChamadoResponse(7L, "Rede", "Sem conexão", LocalDateTime.now(), null,
                Prioridade.ALTA, StatusChamado.ABERTO, 42L, 3L, 9L);
    }
}
