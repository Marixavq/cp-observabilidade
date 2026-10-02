package com.fiap.servico_pedidos.client;

import com.fiap.servico_pedidos.exception.FreteIndisponivelException;
import com.fiap.servico_pedidos.model.Frete;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class FreteClient {

    private static final Logger log = LoggerFactory.getLogger(FreteClient.class);

    private final RestClient freteRestClient;
    private final MeterRegistry registry;
    private final Timer duracao;

    public FreteClient(RestClient freteRestClient, MeterRegistry registry) {
        this.freteRestClient = freteRestClient;
        this.registry = registry;
        this.duracao = Timer.builder("frete.duracao")
                .description("Duracao das chamadas ao servico-frete")
                .publishPercentiles(0.5, 0.95, 0.99)
                .register(registry);
    }

    public Frete consultar(String cep) {
        return chamar(cep, "/frete/{cep}");
    }

    public Frete consultarComFalha(String cep) {
        return chamar(cep, "/frete/{cep}/falha");
    }

    private Frete chamar(String cep, String uri) {
        log.info("Chamando servico-frete: GET {} (cep={})", uri, cep);
        Timer.Sample sample = Timer.start(registry);
        try {
            Frete frete = freteRestClient.get()
                    .uri(uri, cep)
                    .retrieve()
                    .body(Frete.class);
            long ms = toMillis(sample.stop(duracao));
            contarChamada("sucesso");
            log.info("servico-frete respondeu em {} ms: valor={}, transportadora={}",
                    ms, frete.valor(), frete.transportadora());
            return frete;
        } catch (RestClientException e) {
            long ms = toMillis(sample.stop(duracao));
            contarChamada("erro");
            log.warn("Falha ao chamar servico-frete apos {} ms: {}", ms, e.getMessage());
            throw new FreteIndisponivelException("Servico de frete indisponivel", e);
        }
    }

    private void contarChamada(String status) {
        Counter.builder("frete.chamadas")
                .description("Total de chamadas ao servico-frete")
                .tag("status", status)
                .register(registry)
                .increment();
    }

    private static long toMillis(long nanos) {
        return nanos / 1_000_000;
    }
}
