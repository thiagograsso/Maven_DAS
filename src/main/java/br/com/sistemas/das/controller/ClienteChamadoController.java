package br.com.sistemas.das.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.sistemas.das.dto.ChamadoResponse;
import br.com.sistemas.das.service.ChamadoService;

@RestController
@RequestMapping("/clientes/{clienteId}/chamados")
public class ClienteChamadoController {
    private final ChamadoService chamadoService;

    public ClienteChamadoController(ChamadoService chamadoService) {
        this.chamadoService = chamadoService;
    }

    @GetMapping
    public Page<ChamadoResponse> listar(
            @PathVariable Long clienteId,
            @PageableDefault(size = 20, sort = "dataAbertura", direction = Sort.Direction.DESC)
            Pageable pageable) {
        return chamadoService.listarPorCliente(clienteId, pageable);
    }
}
