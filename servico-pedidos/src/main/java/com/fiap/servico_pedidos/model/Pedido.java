package com.fiap.servico_pedidos.model;

public record Pedido(
        Long id,
        String cliente,
        double valorTotal,
        double frete,
        String transportadora,
        long tempoProcessamentoMs
) {
}
