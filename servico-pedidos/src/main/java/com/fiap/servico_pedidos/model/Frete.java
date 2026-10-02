package com.fiap.servico_pedidos.model;

public record Frete(
        String cep,
        double valor,
        String transportadora
) {
}
