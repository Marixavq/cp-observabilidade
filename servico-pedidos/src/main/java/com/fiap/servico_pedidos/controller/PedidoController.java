package com.fiap.servico_pedidos.controller;


import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClient;

import java.util.Map;

@RestController
@RequestMapping("/pedidos")
public class PedidoController {

    private final RestClient restClient;

    public PedidoController(RestClient.Builder builder) {
        this.restClient = builder
                .baseUrl("http://localhost:8081")
                .build();
    }

    @GetMapping("/{id}")
    public Map<String, Object> buscarPedido(@PathVariable Long id) {

        Map<String, Object> frete = restClient
                .get()
                .uri("/frete/01310100")
                .retrieve()
                .body(Map.class);

        return Map.of(
                "id", id,
                "cliente", "Maria Silva",
                "valorTotal", 250.00,
                "frete", frete.get("valor"),
                "transportadora", frete.get("transportadora")
        );
    }
}