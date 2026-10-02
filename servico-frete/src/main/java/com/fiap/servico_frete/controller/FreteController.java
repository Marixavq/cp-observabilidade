package com.fiap.servico_frete.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/frete")
public class FreteController {

    @GetMapping("/{cep}")
    public Map<String, Object> consultarFrete(@PathVariable String cep) {
        return Map.of(
                "cep", cep,
                "valor", 18.90,
                "transportadora", "Entrega Rápida"
        );
    }

    @GetMapping("/{cep}/falha")
    public Map<String, Object> consultarFreteComFalha(@PathVariable String cep) {

        if (Math.random() < 0.3) {
            throw new RuntimeException("Falha ao consultar frete");
        }

        return Map.of(
                "cep", cep,
                "valor", 18.90,
                "transportadora", "Entrega Rápida"
        );
    }
}
