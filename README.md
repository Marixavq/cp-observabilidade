# Checkpoint 5 – Java Avançado: Observabilidade
# INTEGRANTES

Júlia Tiziotto Buttler RM564975
Mariana Xavier Quispe RM566357

Dois microsserviços Spring Boot 4.1 (Java 21) instrumentados com **Actuator + Micrometer** (métricas no formato Prometheus), **Micrometer Tracing + Brave** (traces enviados ao **Zipkin**) e **logs correlacionados** por `traceId`/`spanId`.

| Serviço | Porta | Responsabilidade |
|---|---|---|
| `servico-pedidos` | 8080 | Consulta pedidos e chama o `servico-frete` |
| `servico-frete` | 8081 | Simula cálculo de frete com latência aleatória (200–800 ms) |

```
cliente ──► servico-pedidos ──HTTP──► servico-frete
                │   (traceId propagado via header b3/traceparent)
                ├── métricas ──► /actuator/prometheus
                └── spans ─────► Zipkin (:9411) ◄──────── servico-frete
```

## Como executar

**Pré-requisito:** Java 21 ou superior. Maven não precisa estar instalado, cada serviço tem o Maven Wrapper (`mvnw`).

São necessários 3 processos rodando ao mesmo tempo, cada um em um terminal: **Zipkin**, **servico-frete** e **servico-pedidos**. Um 4º terminal é usado para gerar tráfego. Todos os comandos abaixo partem da raiz do repositório.

### Passo 1 – Baixar e subir o Zipkin (terminal 1)

O Zipkin é distribuído como um jar executável (~135 MB). Baixe **fora da pasta do projeto**, para não ir para o repositório.

PowerShell (Windows):

```powershell
Invoke-WebRequest https://repo1.maven.org/maven2/io/zipkin/zipkin-server/3.6.1/zipkin-server-3.6.1-exec.jar -OutFile $env:USERPROFILE\zipkin.jar
java -jar $env:USERPROFILE\zipkin.jar
```

Bash (Linux/macOS):

```bash
curl -L -o ~/zipkin.jar https://repo1.maven.org/maven2/io/zipkin/zipkin-server/3.6.1/zipkin-server-3.6.1-exec.jar
java -jar ~/zipkin.jar
```

Pronto quando aparecer o logo do Zipkin no terminal. Confira abrindo http://localhost:9411 no navegador.

### Passo 2 – Subir o servico-frete (terminal 2)

```powershell
cd servico-frete
.\mvnw.cmd spring-boot:run      # Windows
./mvnw spring-boot:run          # Linux/macOS
```

Pronto quando aparecer `Started ServicoFreteApplication`.

### Passo 3 – Subir o servico-pedidos (terminal 3)

```powershell
cd servico-pedidos
.\mvnw.cmd spring-boot:run      # Windows
./mvnw spring-boot:run          # Linux/macOS
```

Pronto quando aparecer `Started ServicoPedidosApplication`.

> **No IntelliJ:** em vez dos passos 2 e 3, dá para rodar as configurações `ServicoFreteApplication` e `ServicoPedidosApplication` pelo botão Run. Cada serviço abre em uma aba do painel Run. O Zipkin continua precisando de um terminal.

### Passo 4 – Conferir se tudo subiu (terminal 4)

```powershell
curl.exe http://localhost:8081/actuator/health    # Windows (use "curl" no Linux/macOS)
curl.exe http://localhost:8080/actuator/health
```

Os dois devem responder com `"status":"UP"`.

### Passo 5 – Gerar tráfego (terminal 4)

Faz 20 consultas normais, 10 consultas com chance de falha e 3 consultas lentas. Leva cerca de 30 segundos.

PowerShell:

```powershell
1..20 | % { curl.exe -s http://localhost:8080/pedidos/$_ > $null }
1..10 | % { curl.exe -s http://localhost:8080/pedidos/$_/falha > $null }
1..3  | % { curl.exe -s http://localhost:8080/pedidos/lentos > $null }
```

Bash:

```bash
for i in $(seq 1 20); do curl -s localhost:8080/pedidos/$i > /dev/null; done
for i in $(seq 1 10); do curl -s localhost:8080/pedidos/$i/falha > /dev/null; done
for i in $(seq 1 3);  do curl -s localhost:8080/pedidos/lentos > /dev/null; done
```

Enquanto roda, os terminais 2 e 3 mostram as linhas de log começando com `[traceId,spanId]`.

## Como testar cada requisito

| URL | O que mostra |
|---|---|
| http://localhost:9411 | Zipkin (traces) |
| http://localhost:8080/actuator/prometheus | Métricas do `servico-pedidos` |
| http://localhost:8081/actuator/prometheus | Métricas do `servico-frete` |

### Endpoints (requisitos 4.1 e 4.2)

Abra no navegador (no PowerShell os acentos aparecem quebrados, mas a resposta está correta em UTF-8):

| URL | Resultado esperado |
|---|---|
| http://localhost:8080/pedidos/42 | JSON do pedido com `frete`, `transportadora` e `tempoProcessamentoMs` entre ~200 e ~800 |
| http://localhost:8080/pedidos/lentos | Demora ~2s; `tempoProcessamentoMs` ≈ 2000 |
| http://localhost:8080/pedidos/1/falha | Recarregue (F5) algumas vezes: ~70% das vezes vem o pedido, ~30% vem um **502** com `traceId` no corpo |
| http://localhost:8081/frete/01310100 | Valor e transportadora do frete, com demora de 200–800 ms |
| http://localhost:8081/frete/01310100/falha | ~30% das vezes responde **500** |

### Logs (requisito 5.1)

1. Abra http://localhost:8080/pedidos/1/falha e recarregue até vir o erro 502. Copie o `traceId` do corpo da resposta.
2. Procure esse `traceId` (Ctrl+F) no terminal do `servico-pedidos` e no do `servico-frete`.

Resultado esperado: todas as linhas da requisição nos **dois** serviços começam com o mesmo `traceId`. Há linhas INFO (entrada no controller, chamada ao frete) e WARN (falha). Veja o exemplo na seção [Logs](#logs).

### Métricas (requisito 5.2)

Logo depois de gerar tráfego (passo 5), rode:

PowerShell:

```powershell
(curl.exe -s http://localhost:8080/actuator/prometheus) -split "`n" | Select-String '^(pedidos_consultados|pedidos_duracao|frete_chamadas|frete_duracao)'
```

Bash:

```bash
curl -s localhost:8080/actuator/prometheus | grep -E '^(pedidos_consultados|pedidos_duracao|frete_chamadas|frete_duracao)'
```

Resultado esperado:

- `pedidos_consultados_total` com `status="sucesso"` e `status="erro"`.
- `pedidos_duracao_seconds` com `quantile="0.5"`, `"0.95"` e `"0.99"` para cada `operacao`. Em `operacao="lentos"` os valores ficam em ~2s.
- `frete_chamadas_total` e `frete_duracao_seconds`.
- A soma de `frete_chamadas_total` é igual a `frete_duracao_seconds_count`.

> **Percentis zerados?** O Micrometer calcula os percentis numa janela deslizante de ~2 minutos. Se nenhuma requisição chegou nesse intervalo, eles voltam a `0.0`. Gere tráfego de novo e rode o comando logo em seguida.

Para conferir as métricas automáticas do Actuator, troque o filtro por `'^(http_server_requests_seconds_count|jvm_memory_used_bytes)'`.

### Traces (requisito 5.3)

Em http://localhost:9411:

1. **Trace completo:** clique em **Run Query** e abra um trace `servico-pedidos: get /pedidos/{id}`. Resultado esperado: 4 spans aninhados, sendo eles `http get /pedidos/{id}` (controller), `pedido-service.buscar-pedido` (`@Observed`), `http get` (chamada ao frete) e `http get /frete/{cep}` (do `servico-frete`).
2. **Trace com erro:** cole na caixa de busca do topo o `traceId` copiado no teste de logs. Resultado esperado: os spans `pedido-service.buscar-pedido-com-falha` e `http get` aparecem em vermelho, com a tag `error`.
3. **Trace lento:** abra um trace `get /pedidos/lentos`. Resultado esperado: span de ~2s **sem** chamada ao `servico-frete`.
4. **Dependências:** a aba **Dependencies** mostra `servico-pedidos → servico-frete`.

> Os spans levam alguns segundos para chegar ao Zipkin. Se a busca vier vazia, espere e clique em **Run Query** de novo.

### Problemas comuns

| Problema | Solução |
|---|---|
| `Port 8080 was already in use` | Algum processo ficou preso na porta. No PowerShell: `Get-NetTCPConnection -LocalPort 8080,8081,9411 -State Listen \| % { Stop-Process -Id $_.OwningProcess -Force }`. No Linux: `fuser -k 8080/tcp 8081/tcp 9411/tcp` |
| `mvnw.cmd` / `mvnw` não encontrado | O comando precisa ser rodado de dentro da pasta do serviço |
| `/pedidos/42` responde 502 sempre | O `servico-frete` não está rodando |
| Percentis em `0.0` | Ver a nota na seção de métricas |
| Zipkin sem traces | Confira se o Zipkin subiu antes do tráfego e espere alguns segundos |

Para encerrar, use **Ctrl+C** em cada terminal.

## Endpoints

### servico-pedidos

| Método | Rota | Descrição |
|---|---|---|
| GET | `/pedidos/{id}` | Retorna o pedido; consulta `/frete/{cep}` no `servico-frete` |
| GET | `/pedidos/{id}/falha` | Igual ao anterior, mas consulta `/frete/{cep}/falha`. Responde **502** quando o frete falha |
| GET | `/pedidos/lentos` | Endpoint propositalmente lento (~2s) |

Resposta de `GET /pedidos/42`:

```json
{ "id": 42, "cliente": "Maria Silva", "valorTotal": 250.0, "frete": 18.9,
  "transportadora": "Entrega Rápida", "tempoProcessamentoMs": 312 }
```

Resposta de erro (o `traceId` permite achar o trace no Zipkin e as linhas de log):

```json
{ "timestamp": "2026-10-02T19:40:58.639Z", "status": 502,
  "erro": "Servico de frete indisponivel", "traceId": "6ac008ca1733d8a0b7a8ab64fdfb26a8" }
```

### servico-frete

| Método | Rota | Descrição |
|---|---|---|
| GET | `/frete/{cep}` | Valor e transportadora, com `Thread.sleep` aleatório de 200–800 ms |
| GET | `/frete/{cep}/falha` | Erro 500 em 30% das chamadas |

## Observabilidade

### Logs

Todas as linhas começam com `[traceId,spanId]` (`logging.pattern.console`). Há logs INFO na entrada do controller, na chamada ao serviço externo e no sucesso; WARN em erro ou lentidão. Exemplo real de uma requisição com falha, nos dois serviços:

```
[6ac008ca1733d8a0b7a8ab64fdfb26a8,b7a8ab64fdfb26a8] ... INFO [servico-pedidos] c.f.s.controller.PedidoController : GET /pedidos/2/falha recebido
[6ac008ca1733d8a0b7a8ab64fdfb26a8,5d66fbd1566719bb] ... INFO [servico-pedidos] c.f.s.service.PedidoService : Processando pedido 2
[6ac008ca1733d8a0b7a8ab64fdfb26a8,5d66fbd1566719bb] ... INFO [servico-pedidos] c.f.s.client.FreteClient : Chamando servico-frete: GET /frete/{cep}/falha (cep=01310100)
[6ac008ca1733d8a0b7a8ab64fdfb26a8,189ee1e50f538e67] ... INFO [servico-frete]   c.f.s.controller.FreteController : GET /frete/01310100/falha recebido
[6ac008ca1733d8a0b7a8ab64fdfb26a8,189ee1e50f538e67] ... WARN [servico-frete]   c.f.s.controller.FreteController : Falha simulada ao calcular frete para o CEP 01310100
[6ac008ca1733d8a0b7a8ab64fdfb26a8,5d66fbd1566719bb] ... WARN [servico-pedidos] c.f.s.client.FreteClient : Falha ao chamar servico-frete apos 452 ms: 500 Internal Server Error
[6ac008ca1733d8a0b7a8ab64fdfb26a8,5d66fbd1566719bb] ... WARN [servico-pedidos] c.f.s.service.PedidoService : Erro ao processar pedido 2: Servico de frete indisponivel
[6ac008ca1733d8a0b7a8ab64fdfb26a8,b7a8ab64fdfb26a8] ... WARN [servico-pedidos] c.f.s.e.GlobalExceptionHandler : Respondendo 502: Servico de frete indisponivel
```

O mesmo `traceId` aparece nos dois serviços; o `spanId` muda conforme a requisição passa pelo controller, pelo método `@Observed` e pelo `servico-frete`.

### Métricas

Expostas em `/actuator/prometheus` do `servico-pedidos`:

| Métrica (Prometheus) | Tipo | Tags | Onde é registrada |
|---|---|---|---|
| `pedidos_consultados_total` | Counter | `status` = `sucesso`/`erro` | `PedidoService` |
| `pedidos_duracao_seconds` | Timer (p50, p95, p99) | `operacao` = `buscar`/`buscar-com-falha`/`lentos` | `PedidoService` |
| `frete_chamadas_total` | Counter | `status` = `sucesso`/`erro` | `FreteClient` |
| `frete_duracao_seconds` | Timer (p50, p95, p99) | – | `FreteClient` |

> **Sobre o sufixo:** o enunciado cita `pedidos_duracao_segundos`. O Micrometer sempre exporta Timers na unidade base do Prometheus, adicionando o sufixo `_seconds` ao nome (`pedidos.duracao` → `pedidos_duracao_seconds`). É a mesma métrica.

As métricas automáticas continuam disponíveis (`http_server_requests_seconds`, `jvm_memory_used_bytes`, etc.), além das geradas pelo `@Observed` (`pedidos_service_*`).

### Traces

Cada requisição a `GET /pedidos/{id}` gera um trace com 4 spans, visível no Zipkin:

```
servico-pedidos  SERVER  http get /pedidos/{id}               272 ms   ← span do controller
└─ servico-pedidos       pedido-service.buscar-pedido         270 ms   ← método com @Observed
   └─ servico-pedidos  CLIENT  http get                       270 ms   ← chamada HTTP (RestClient)
      └─ servico-frete  SERVER  http get /frete/{cep}         267 ms   ← lado do servico-frete
```

Em `/pedidos/{id}/falha`, quando o frete falha, os spans do `@Observed` e da chamada HTTP ficam marcados com a tag `error`.

## Diagnóstico do cenário de lentidão

Valores reais coletados após algumas requisições:

```
pedidos_duracao_seconds{operacao="buscar",quantile="0.95"}   0.956
pedidos_duracao_seconds{operacao="lentos",quantile="0.95"}   2.013
frete_duracao_seconds{quantile="0.95"}                       0.931
```

**Qual endpoint está mais lento?** Pelo `pedidos_duracao_seconds` (e por `http_server_requests_seconds` por `uri`), `/pedidos/lentos` tem p50/p95/p99 em ~2s, contra menos de 1s de `/pedidos/{id}`.

**A lentidão é do meu serviço ou de um serviço externo?** Os traces respondem:

```
http get /pedidos/{id}        272 ms
└─ buscar-pedido              270 ms
   └─ http get (frete)        270 ms   ← quase 100% do tempo está na chamada ao servico-frete

http get /pedidos/lentos     2017 ms
└─ listar-lentos             2015 ms   ← nenhum span filho: o tempo é gasto dentro do próprio servico-pedidos
```

- Em `/pedidos/{id}`, o tempo está todo na chamada ao `servico-frete`, e `frete_duracao_seconds` tem p95 próximo do p95 de `pedidos_duracao_seconds{operacao="buscar"}`. **A lentidão é do serviço externo** (o `Thread.sleep` de 200–800 ms do frete).
- Em `/pedidos/lentos`, o span do `@Observed` dura ~2s e não tem nenhuma chamada externa. **O gargalo é interno** ao `servico-pedidos`.

**Qual foi o caminho exato da requisição que falhou?** A resposta 502 traz o `traceId`. Buscando-o no Zipkin, aparece o caminho controller → `@Observed` → chamada HTTP → `servico-frete`, com o erro marcado nos spans. Buscando o mesmo `traceId` nos logs, aparecem todas as linhas dos dois serviços daquela requisição, incluindo o WARN `Falha simulada ao calcular frete` no `servico-frete`.
