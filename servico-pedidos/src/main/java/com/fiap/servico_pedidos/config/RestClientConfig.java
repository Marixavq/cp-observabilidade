package com.fiap.servico_pedidos.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

    // O RestClient.Builder precisa ser o auto-configurado pelo Spring: ele já vem com o
    // ObservationRegistry, que gera o span da chamada HTTP e propaga o traceId para o frete.
    @Bean
    public RestClient freteRestClient(RestClient.Builder builder, @Value("${frete.url}") String freteUrl) {
        return builder
                .baseUrl(freteUrl)
                .build();
    }
}
