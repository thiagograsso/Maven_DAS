package br.com.sistemas.das.controller;

import java.net.URI;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.sistemas.das.dto.AtribuirTecnicoRequest;
import br.com.sistemas.das.dto.AlterarStatusRequest;
import br.com.sistemas.das.dto.ChamadoDetalheResponse;
import br.com.sistemas.das.dto.ChamadoRequest;
import br.com.sistemas.das.dto.ChamadoResponse;
import br.com.sistemas.das.dto.ComentarioRequest;
import br.com.sistemas.das.dto.ComentarioResponse;
import br.com.sistemas.das.entity.Prioridade;
import br.com.sistemas.das.entity.StatusChamado;
import br.com.sistemas.das.service.ChamadoService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/chamados")
public class ChamadoController {
    private final ChamadoService service;

    public ChamadoController(ChamadoService service) {
        this.service = service;
    }

    @GetMapping
    public Page<ChamadoResponse> listar(
            @RequestParam(required = false) StatusChamado status,
            @RequestParam(required = false) Prioridade prioridade,
            @RequestParam(required = false) Long clienteId,
            @PageableDefault(size = 20, sort = "dataAbertura", direction = Sort.Direction.DESC)
            Pageable pageable) {
        return service.listar(status, prioridade, clienteId, pageable);
    }

    @GetMapping("/{id}")
    public ChamadoDetalheResponse buscar(@PathVariable Long id) {
        return service.buscar(id);
    }

    @PostMapping
    public ResponseEntity<ChamadoResponse> criar(@Valid @RequestBody ChamadoRequest request) {
        ChamadoResponse response = service.criar(request);
        return ResponseEntity.created(URI.create("/chamados/" + response.id())).body(response);
    }

    @PutMapping("/{id}")
    public ChamadoResponse atualizar(@PathVariable Long id, @Valid @RequestBody ChamadoRequest request) {
        return service.atualizar(id, request);
    }

    @PatchMapping("/{id}/atribuir")
    public ChamadoResponse atribuir(
            @PathVariable Long id, @Valid @RequestBody AtribuirTecnicoRequest request) {
        return service.atribuir(id, request);
    }

    @PatchMapping("/{id}/status")
    public ChamadoResponse alterarStatus(
            @PathVariable Long id, @Valid @RequestBody AlterarStatusRequest request) {
        return service.alterarStatus(id, request);
    }

    @PostMapping("/{id}/comentarios")
    public ResponseEntity<ComentarioResponse> comentar(
            @PathVariable Long id, @Valid @RequestBody ComentarioRequest request) {
        return ResponseEntity.status(201).body(service.adicionarComentario(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> excluir(@PathVariable Long id) {
        service.excluir(id);
        return ResponseEntity.noContent().build();
    }
}
