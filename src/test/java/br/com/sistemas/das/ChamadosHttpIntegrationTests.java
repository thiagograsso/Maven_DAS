package br.com.sistemas.das;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import br.com.sistemas.das.entity.Categoria;
import br.com.sistemas.das.entity.Cliente;
import br.com.sistemas.das.entity.Perfil;
import br.com.sistemas.das.entity.Tecnico;
import br.com.sistemas.das.repository.CategoriaRepository;
import br.com.sistemas.das.repository.ChamadoRepository;
import br.com.sistemas.das.repository.ClienteRepository;
import br.com.sistemas.das.repository.ComentarioRepository;
import br.com.sistemas.das.repository.TecnicoRepository;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ChamadosHttpIntegrationTests {
    private final HttpClient http = HttpClient.newHttpClient();

    @LocalServerPort
    private int port;

    @Autowired
    private ChamadoRepository chamadoRepository;

    @Autowired
    private ComentarioRepository comentarioRepository;

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private CategoriaRepository categoriaRepository;

    @Autowired
    private TecnicoRepository tecnicoRepository;

    private Cliente cliente;
    private Categoria categoria;
    private Tecnico tecnico;
    private Tecnico outroTecnico;

    @BeforeEach
    void prepararDados() {
        comentarioRepository.deleteAll();
        chamadoRepository.deleteAll();
        categoriaRepository.deleteAll();
        tecnicoRepository.deleteAll();
        clienteRepository.deleteAll();

        cliente = clienteRepository.save(new Cliente("Cliente HTTP", "http@example.com", null));
        categoria = categoriaRepository.save(new Categoria("HTTP", "Teste de rotas"));
        tecnico = tecnicoRepository.save(new Tecnico("Ana", "ana-http@example.com", Perfil.TECNICO));
        outroTecnico = tecnicoRepository.save(
                new Tecnico("Carlos", "carlos-http@example.com", Perfil.TECNICO));
    }

    @Test
    void atendeFluxoHttpDeCriacaoListagemAtribuicaoStatusEComentario() throws Exception {
        HttpResponse<String> criado = enviar("POST", "/chamados", """
                {"titulo":"Falha de rede","descricao":"Sem conexão","prioridade":"ALTA",
                 "clienteId":%d,"categoriaId":%d,"tecnicoId":%d}
                """.formatted(cliente.getId(), categoria.getId(), tecnico.getId()));
        assertThat(criado.statusCode()).isEqualTo(201);
        Long chamadoId = idDaResposta(criado.body());

        assertThat(enviar("GET", "/chamados?page=0&size=10", null).statusCode()).isEqualTo(200);
        assertThat(enviar("GET", "/chamados?status=ABERTO&prioridade=ALTA&clienteId="
                + cliente.getId() + "&page=0&size=10&sort=dataAbertura,desc", null).statusCode()).isEqualTo(200);
        assertThat(enviar("GET", "/clientes/" + cliente.getId() + "/chamados?page=0&size=10", null)
                .statusCode()).isEqualTo(200);
        assertThat(enviar("GET", "/chamados/" + chamadoId, null).statusCode()).isEqualTo(200);
        assertThat(enviar("GET", "/chamados?size=101", null).statusCode()).isEqualTo(400);
        assertThat(enviar("GET", "/chamados?sort=campoInvalido,asc", null).statusCode()).isEqualTo(400);

        HttpResponse<String> atribuido = enviar("PATCH", "/chamados/" + chamadoId + "/atribuir",
                "{\"tecnicoId\":" + outroTecnico.getId() + "}");
        assertThat(atribuido.statusCode()).isEqualTo(200);
        assertThat(atribuido.body()).contains("\"tecnicoId\":" + outroTecnico.getId());

        assertThat(enviar("PATCH", "/chamados/" + chamadoId + "/status",
                "{\"status\":\"EM_ATENDIMENTO\"}").statusCode()).isEqualTo(200);

        HttpResponse<String> comentario = enviar("POST", "/chamados/" + chamadoId + "/comentarios",
                "{\"texto\":\"Cliente confirmou.\",\"autor\":\"Ana\"}");
        assertThat(comentario.statusCode()).isEqualTo(201);
        assertThat(comentario.body()).contains("\"dataCriacao\":");

        HttpResponse<String> fechado = enviar("PATCH", "/chamados/" + chamadoId + "/status",
                "{\"status\":\"FECHADO\"}");
        assertThat(fechado.statusCode()).isEqualTo(200);
        assertThat(fechado.body()).contains("\"dataFechamento\":");

        HttpResponse<String> detalhe = enviar("GET", "/chamados/" + chamadoId, null);
        assertThat(detalhe.statusCode()).isEqualTo(200);
        assertThat(detalhe.body()).contains("\"comentarios\":[", "\"texto\":\"Cliente confirmou.\"");

        assertThat(enviar("PATCH", "/chamados/" + chamadoId + "/atribuir",
                "{\"tecnicoId\":" + tecnico.getId() + "}").statusCode()).isEqualTo(409);
        assertThat(enviar("PUT", "/chamados/" + chamadoId, """
                {"titulo":"Falha alterada","descricao":"Tentativa após fechamento","prioridade":"ALTA",
                 "clienteId":%d,"categoriaId":%d,"tecnicoId":%d}
                """.formatted(cliente.getId(), categoria.getId(), tecnico.getId())).statusCode()).isEqualTo(409);
        assertThat(enviar("POST", "/chamados/" + chamadoId + "/comentarios",
                "{\"texto\":\"Comentário após fechamento\",\"autor\":\"Ana\"}").statusCode()).isEqualTo(201);
    }

    @Test
    void atendeCadastroHttpDeClientes() throws Exception {
        HttpRequest paginaRequest = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/"))
                .header("Accept", "text/html")
                .GET()
                .build();
        HttpResponse<String> pagina = http.send(paginaRequest, HttpResponse.BodyHandlers.ofString());
        assertThat(pagina.statusCode()).isEqualTo(200);
        assertThat(pagina.body()).contains("Atende — Central de chamados", "/app.js", "/styles.css");

        HttpResponse<String> criado = enviar("POST", "/clientes",
                "{\"nome\":\"Maria Souza\",\"email\":\"maria@example.com\",\"telefone\":\"11999990000\"}");
        assertThat(criado.statusCode()).isEqualTo(201);
        assertThat(criado.body()).contains("\"nome\":\"Maria Souza\"", "\"telefone\":\"11999990000\"");
        Long clienteId = idDaResposta(criado.body());

        assertThat(enviar("GET", "/clientes", null).body()).contains("\"id\":" + clienteId);
        assertThat(enviar("GET", "/clientes/" + clienteId, null).statusCode()).isEqualTo(200);

        HttpResponse<String> atualizado = enviar("PUT", "/clientes/" + clienteId,
                "{\"nome\":\"Maria Silva\",\"email\":\"maria@example.com\",\"telefone\":\"11888880000\"}");
        assertThat(atualizado.statusCode()).isEqualTo(200);
        assertThat(atualizado.body()).contains("\"nome\":\"Maria Silva\"", "\"telefone\":\"11888880000\"");

        assertThat(enviar("POST", "/clientes",
                "{\"nome\":\"Outro Cliente\",\"email\":\"maria@example.com\"}").statusCode()).isEqualTo(409);
        assertThat(enviar("DELETE", "/clientes/" + clienteId, null).statusCode()).isEqualTo(204);
        assertThat(enviar("GET", "/clientes/" + clienteId, null).statusCode()).isEqualTo(404);
    }

    private HttpResponse<String> enviar(String method, String path, String json) throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder(
                URI.create("http://localhost:" + port + path)).header("Accept", "application/json");
        if (json == null) {
            request.method(method, HttpRequest.BodyPublishers.noBody());
        } else {
            request.header("Content-Type", "application/json")
                    .method(method, HttpRequest.BodyPublishers.ofString(json));
        }
        return http.send(request.build(), HttpResponse.BodyHandlers.ofString());
    }

    private Long idDaResposta(String response) {
        return Long.parseLong(response.replaceFirst(".*\\\"id\\\":(\\d+).*", "$1"));
    }
}
