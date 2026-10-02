package com.fiap.servico_pedidos.controller;

import com.fiap.servico_pedidos.model.Pedido;
import com.fiap.servico_pedidos.service.PedidoService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/pedidos")
public class PedidoController {

    private static final Logger log = LoggerFactory.getLogger(PedidoController.class);

    private final PedidoService pedidoService;

    public PedidoController(PedidoService pedidoService) {
        this.pedidoService = pedidoService;
    }

    @GetMapping("/{id}")
    public Pedido buscarPedido(@PathVariable Long id) {
        log.info("GET /pedidos/{} recebido", id);
        return pedidoService.buscarPedido(id);
    }

    @GetMapping("/{id}/falha")
    public Pedido buscarPedidoComFalha(@PathVariable Long id) {
        log.info("GET /pedidos/{}/falha recebido", id);
        return pedidoService.buscarPedidoComFalha(id);
    }

    @GetMapping("/lentos")
    public List<Pedido> listarPedidosLentos() {
        log.info("GET /pedidos/lentos recebido");
        return pedidoService.listarPedidosLentos();
    }
}
