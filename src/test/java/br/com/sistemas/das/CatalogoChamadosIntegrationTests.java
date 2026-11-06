package br.com.sistemas.das;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import br.com.sistemas.das.entity.Categoria;
import br.com.sistemas.das.entity.Cliente;
import br.com.sistemas.das.entity.Tecnico;
import br.com.sistemas.das.entity.Perfil;
import br.com.sistemas.das.repository.CategoriaRepository;
import br.com.sistemas.das.repository.ChamadoRepository;
import br.com.sistemas.das.repository.ClienteRepository;
import br.com.sistemas.das.repository.TecnicoRepository;
import jakarta.persistence.EntityManager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CatalogoChamadosIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CategoriaRepository categoriaRepository;

    @Autowired
    private TecnicoRepository tecnicoRepository;

    @Autowired
    private ChamadoRepository chamadoRepository;

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private EntityManager entityManager;

    @BeforeEach
    void limparDados() {
        chamadoRepository.deleteAll();
        categoriaRepository.deleteAll();
        tecnicoRepository.deleteAll();
        clienteRepository.deleteAll();
    }

    @Test
    void categoriaCrudEImpedeNomeDuplicadoIgnorandoMaiusculas() throws Exception {
        String request = """
                {"nome":"Hardware","descricao":"Equipamentos"}
                """;
        String response = mockMvc.perform(post("/categorias")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.nome", is("Hardware")))
                .andReturn().getResponse().getContentAsString();
        Long id = idDaResposta(response);

        mockMvc.perform(post("/categorias")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome":"HARDWARE","descricao":"Duplicada"}
                                """))
                .andExpect(status().isConflict());
        mockMvc.perform(get("/categorias/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(id.intValue())));
        mockMvc.perform(put("/categorias/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome":"Infraestrutura","descricao":"Atualizada"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nome", is("Infraestrutura")));
        mockMvc.perform(delete("/categorias/{id}", id))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/categorias/{id}", id))
                .andExpect(status().isNotFound());
    }

    @Test
    void tecnicoCrudEImpedeEmailDuplicado() throws Exception {
        String response = mockMvc.perform(post("/tecnicos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome":"Ana","email":"ana@example.com","perfil":"ADMIN"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.perfil", is("ADMIN")))
                .andReturn().getResponse().getContentAsString();
        Long id = idDaResposta(response);

        mockMvc.perform(post("/tecnicos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome":"Outra Ana","email":"ana@example.com","perfil":"TECNICO"}
                                """))
                .andExpect(status().isConflict());
        mockMvc.perform(get("/tecnicos/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email", is("ana@example.com")));
        mockMvc.perform(put("/tecnicos/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome":"Ana Silva","email":"ana@example.com","perfil":"TECNICO"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.perfil", is("TECNICO")));
        mockMvc.perform(delete("/tecnicos/{id}", id))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/tecnicos/{id}", id))
                .andExpect(status().isNotFound());
    }

    @Test
    void chamadoPersisteRelacoesEDefaultsDeAbertura() throws Exception {
        Cliente cliente = clienteRepository.save(new Cliente("Cliente Teste", "cliente@example.com", null));
        Categoria categoria = categoriaRepository.save(new Categoria("Rede", "Conectividade"));
        Tecnico tecnico = tecnicoRepository.save(new Tecnico("João", "joao@example.com", Perfil.TECNICO));
        entityManager.flush();
        entityManager.clear();

        String response = mockMvc.perform(post("/chamados")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "titulo":"Sem conexão",
                                  "descricao":"Falha de acesso à rede",
                                  "prioridade":"ALTA",
                                  "clienteId":%d,
                                  "categoriaId":%d,
                                  "tecnicoId":%d
                                }
                                """.formatted(cliente.getId(), categoria.getId(), tecnico.getId())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status", is("ABERTO")))
                .andExpect(jsonPath("$.prioridade", is("ALTA")))
                .andExpect(jsonPath("$.clienteId", is(cliente.getId().intValue())))
                .andExpect(jsonPath("$.categoriaId", is(categoria.getId().intValue())))
                .andExpect(jsonPath("$.tecnicoId", is(tecnico.getId().intValue())))
                .andReturn().getResponse().getContentAsString();

        Long chamadoId = idDaResposta(response);
        assertNotNull(chamadoRepository.findById(chamadoId).orElseThrow().getDataAbertura());
        entityManager.clear();
        var persistido = chamadoRepository.findById(chamadoId).orElseThrow();
        assertEquals(cliente.getId(), persistido.getCliente().getId());
        assertEquals(categoria.getId(), persistido.getCategoria().getId());
        assertEquals(tecnico.getId(), persistido.getTecnico().getId());
        assertEquals("ABERTO", persistido.getStatus().name());
        mockMvc.perform(get("/chamados/{id}", chamadoId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.titulo", is("Sem conexão")));
    }

    private Long idDaResposta(String response) throws Exception {
        return Long.parseLong(response.replaceFirst(".*\\\"id\\\":(\\d+).*", "$1"));
    }
}
