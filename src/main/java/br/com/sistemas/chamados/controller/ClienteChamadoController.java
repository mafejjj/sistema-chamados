package br.com.sistemas.chamados.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.sistemas.chamados.dto.ChamadoResponse;
import br.com.sistemas.chamados.service.ChamadoService;

@RestController
@RequestMapping("/clientes/{clienteId}/chamados")
public class ClienteChamadoController {

    private final ChamadoService service;

    public ClienteChamadoController(ChamadoService service) {
        this.service = service;
    }

    @GetMapping
    public Page<ChamadoResponse> listar(
        @PathVariable Long clienteId, Pageable pageable) {
        return service.listarPorCliente(clienteId, pageable);
    }
}