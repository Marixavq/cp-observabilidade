package com.fiap.servico_frete.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

@RestController
@RequestMapping("/frete")
public class FreteController {

    private static final Logger log = LoggerFactory.getLogger(FreteController.class);

    private static final double CHANCE_FALHA = 0.3;

    @GetMapping("/{cep}")
    public Map<String, Object> consultarFrete(@PathVariable String cep) {
        log.info("GET /frete/{} recebido", cep);
        simularLatencia();
        return calcular(cep);
    }

    @GetMapping("/{cep}/falha")
    public Map<String, Object> consultarFreteComFalha(@PathVariable String cep) {
        log.info("GET /frete/{}/falha recebido", cep);
        simularLatencia();
        if (ThreadLocalRandom.current().nextDouble() < CHANCE_FALHA) {
            log.warn("Falha simulada ao calcular frete para o CEP {}", cep);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Falha ao consultar frete");
        }
        return calcular(cep);
    }

    private Map<String, Object> calcular(String cep) {
        Map<String, Object> frete = Map.of(
                "cep", cep,
                "valor", 18.90,
                "transportadora", "Entrega Rápida"
        );
        log.info("Frete calculado para o CEP {}: {}", cep, frete.get("valor"));
        return frete;
    }

    private void simularLatencia() {
        long ms = ThreadLocalRandom.current().nextLong(200, 801);
        log.info("Simulando latencia de {} ms", ms);
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Calculo de frete interrompido", e);
        }
    }
}
