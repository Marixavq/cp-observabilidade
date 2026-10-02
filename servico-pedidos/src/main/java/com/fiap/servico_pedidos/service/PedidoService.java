package com.fiap.servico_pedidos.service;

import com.fiap.servico_pedidos.client.FreteClient;
import com.fiap.servico_pedidos.model.Frete;
import com.fiap.servico_pedidos.model.Pedido;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.observation.annotation.Observed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PedidoService {

    private static final Logger log = LoggerFactory.getLogger(PedidoService.class);

    private static final String CEP_PADRAO = "01310100";
    private static final long LIMITE_LENTIDAO_MS = 1000;

    private final FreteClient freteClient;
    private final MeterRegistry registry;

    public PedidoService(FreteClient freteClient, MeterRegistry registry) {
        this.freteClient = freteClient;
        this.registry = registry;
    }

    @Observed(name = "pedidos.service.buscar", contextualName = "pedido-service.buscar-pedido")
    public Pedido buscarPedido(Long id) {
        return processar(id, "buscar", false);
    }

    @Observed(name = "pedidos.service.buscar-com-falha", contextualName = "pedido-service.buscar-pedido-com-falha")
    public Pedido buscarPedidoComFalha(Long id) {
        return processar(id, "buscar-com-falha", true);
    }

    @Observed(name = "pedidos.service.lentos", contextualName = "pedido-service.listar-lentos")
    public List<Pedido> listarPedidosLentos() {
        log.info("Processando consulta lenta de pedidos");
        long inicio = System.nanoTime();
        Timer.Sample sample = Timer.start(registry);
        try {
            Thread.sleep(2000);
            long ms = (System.nanoTime() - inicio) / 1_000_000;
            contarConsulta("sucesso");
            log.warn("Consulta lenta de pedidos levou {} ms (limite {} ms)", ms, LIMITE_LENTIDAO_MS);
            return List.of(
                    new Pedido(1L, "Maria Silva", 250.00, 18.90, "Entrega Rápida", ms),
                    new Pedido(2L, "João Souza", 99.90, 18.90, "Entrega Rápida", ms)
            );
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            contarConsulta("erro");
            throw new IllegalStateException("Consulta lenta interrompida", e);
        } finally {
            sample.stop(timerDuracao("lentos"));
        }
    }

    private Pedido processar(Long id, String operacao, boolean simularFalha) {
        log.info("Processando pedido {}", id);
        long inicio = System.nanoTime();
        Timer.Sample sample = Timer.start(registry);
        try {
            Frete frete = simularFalha
                    ? freteClient.consultarComFalha(CEP_PADRAO)
                    : freteClient.consultar(CEP_PADRAO);
            long ms = (System.nanoTime() - inicio) / 1_000_000;
            Pedido pedido = new Pedido(id, "Maria Silva", 250.00, frete.valor(), frete.transportadora(), ms);
            contarConsulta("sucesso");
            if (ms > LIMITE_LENTIDAO_MS) {
                log.warn("Pedido {} processado com lentidao: {} ms", id, ms);
            } else {
                log.info("Pedido {} processado com sucesso em {} ms", id, ms);
            }
            return pedido;
        } catch (RuntimeException e) {
            contarConsulta("erro");
            log.warn("Erro ao processar pedido {}: {}", id, e.getMessage());
            throw e;
        } finally {
            sample.stop(timerDuracao(operacao));
        }
    }

    private void contarConsulta(String status) {
        Counter.builder("pedidos.consultados")
                .description("Total de pedidos consultados")
                .tag("status", status)
                .register(registry)
                .increment();
    }

    private Timer timerDuracao(String operacao) {
        return Timer.builder("pedidos.duracao")
                .description("Duracao do processamento de pedidos")
                .tag("operacao", operacao)
                .publishPercentiles(0.5, 0.95, 0.99)
                .register(registry);
    }
}
