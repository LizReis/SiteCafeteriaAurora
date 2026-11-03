# Etapa 2 — Pedidos persistidos e imagens

## 1. Base e arquivos

Implementação iniciada na branch `etapa-2-pedidos`, a partir da `main` `4a4c6ff` (merge do PR #2). A Etapa 1 foi reutilizada. Não foi criado worktree nem substituído o catálogo.

| Arquivo sob `src/main/java/br/com/cafeaurora` | Alteração |
|---|---|
| `model/Pedido.java` | Construção consistente, itens encapsulados, contato/e-mail, subtotal/taxa, referência segura, hashes de idempotência/requisição/cotação |
| `model/ItemPedido.java` | Associação ao pedido/produto e snapshot histórico de nome, preço e imagem; quantidade validada |
| `model/CotacaoPedido.java` (novo) | Cotação expira em 15 minutos; armazena somente hash do token e hash do resumo oficial |
| `repository/PedidoRepository.java` | Consultas com itens/produtos carregados e verificação de cotação utilizada |
| `repository/ProdutoRepository.java` | Uma consulta em lote com categoria e bloqueio transacional para preços/condições |
| `repository/CotacaoPedidoRepository.java` (novo) | Persistência das cotações |
| `service/PedidoService.java` (novo) | Validação de domínio, cotação, cálculos, persistência e recuperação idempotente |
| `controller/PedidoController.java` (novo) | HTTP/DTO, status 201 e respostas sem cache |
| `dto/PedidoRequest`, `ItemPedidoRequest`, `EnderecoRequest`, `CotacaoRequest` (novos) | Contratos validados, sem preços/status/totais fornecidos pelo cliente |
| `dto/PedidoResponse`, `ItemPedidoResponse`, `CotacaoResponse` (novos) | Valores oficiais, históricos e dados mínimos de acompanhamento |
| `dto/CardapioProduto.java` | Reutiliza sanitização de URLs de imagem no resumo, sem mudar consultas da Etapa 1 |
| `exception/ApiExceptionHandler.java`, `PedidoPersistenciaException.java` (novo) | JSON inválido/extra e falhas de persistência com mensagens seguras |
| `service/CargaCardapioDev.java` | Espresso passa a usar fotografia compatível existente |
| `controller/PaginaController.java` | Taxa configurada no servidor também prepara a sacola |

Outros arquivos:

- `application.properties`: taxa de entrega e ocultação de detalhes internos nos erros.
- `templates/fragments/layout.html`: metas CSRF e política de referrer, sem alteração de layout.
- `templates/finalizar-pedido.html`: mensagens reais, atualização de resumo e intenção de cartão explicitamente crédito; campos financeiros continuam desabilitados.
- `templates/pedido-confirmado.html`: remove controles demonstrativos de status, identifica consulta real e preserva xícara, detalhes e avaliação.
- `templates/sacola-pedido.html`: taxa em centavos recebida do servidor.
- `static/js/finalizar-pedido.js`: cotação, POST real, idempotência, revisão de conflitos e recuperação de falhas.
- `static/js/pedido-confirmado.js`: consulta por referência, valores persistidos e status somente do servidor.
- `static/js/pedido-avaliacao.js`: acompanha o status real, mantendo formulário e transições existentes.
- `static/js/cart-store.js`: no checkout não remove silenciosamente itens indisponíveis antes de o servidor apresentar o conflito.
- `static/js/sacola-pedido.js`: elimina taxa monetária fixa no JavaScript.
- `src/test/java/.../PedidoIntegracaoTests.java` e `model/PedidoTest.java`: novos testes de domínio, HTTP, banco, CSRF, idempotência, concorrência e rollback.
- `scripts/test-pedidos.cjs`: E2E com Chromium, SQL/JDBC, perda de resposta e reinícios, usando somente bancos temporários.
- `README.md` e este documento: contratos e execução.

Nenhuma dependência nova foi adicionada. Os três designs de cardápio, SVGs e animações foram preservados. O CSS do checkout recebeu apenas uma regra para que a ação de recuperação do resumo tenha a aparência dos links existentes.

## 2. H2 e compatibilidade

Mantém-se `jdbc:h2:file:./data/cafeaurora` com `ddl-auto=update`. **Nenhum banco existente foi apagado ou recriado.** Os testes destrutivos utilizam banco em memória ou arquivos temporários. O arquivo existente foi inspecionado por meio de cópia.

Colunas novas em `pedidos`: `email_cliente`, `subtotal`, `taxa_entrega`, `referencia_publica`, `idempotencia_hash`, `requisicao_hash`, `cotacao_hash`. Em `itens_pedido`, adiciona-se `imagem_url` para histórico. São anuláveis na definição JPA para que registros anteriores não impeçam o início. Pedidos novos são completos por construção. Referência, hash de idempotência e hash da cotação têm unicidade no banco; registros legados podem manter NULL.

A tabela nova `cotacoes_pedido` guarda hash de token, hash do resumo e expiração. Não contém nome, telefone, e-mail nem endereço do cliente. Cotações vencidas não podem gerar novos pedidos. Uma cotação só pode originar um pedido, mesmo com outra chave de idempotência.

Pedidos antigos não recebem referências públicas automaticamente e não são tornados públicos. Subtotal/taxa ausentes não são inventados ou preenchidos com valores presumidos. Total e itens históricos anteriores são preservados. O teste de compatibilidade cria um pedido/item no esquema antigo **somente em uma cópia**, abre a versão nova duas vezes e confere sua preservação por JDBC.

## 3. Imagens: inspeção e correção segura

Os arquivos foram inspecionados visualmente, incluindo os designs anteriores no histórico Git. Vários `alt`/nomes de produtos dos antigos templates não correspondiam ao conteúdo das fotos; restaurar cegamente aqueles caminhos associaria bebidas a fotos de croissant ou bolo.

| Produto demonstrativo novo | URL compatível |
|---|---|
| Espresso Italiano | `/img/cupEsquerdaCima.jpg` (café extraído na máquina) |
| Latte Macchiato | `/img/42953.jpg` (café/leite em camadas no copo) |
| Cappuccino | `/img/1050.jpg` (bebida com leite e arte na xícara) |
| Coquetel de Frutas Vermelhas | `/img/5493.jpg` |
| Coquetel de Café | `/img/9492.jpg` |
| Suco de Laranja | `/img/20691.jpg` |
| Sanduíche da Casa | `/img/4368.jpg` |
| Croissant de Queijo | `/img/1320.jpg` |
| Torta de Frutas Silvestres | `/img/2151900810.jpg` |

`2148354703.jpg` mostra croissants; `2150649262.jpg`, bolo de chocolate; `21690.jpg`, bolo em camadas; `cardapioComida.jpg`, brownie. Não são fotos adequadas para os coquetéis originais ou qualquer produto indistintamente. O fallback neutro permanece quando não há foto confirmada.

A cópia do H2 existente neste ambiente contém nove produtos anteriores, todos com `imagem_url=NULL`: Espresso, Cappuccino, Latte, Frappuccino de Chocolate, Soda Italiana de Frutas Vermelhas, Cold Brew com Caramelo, Croissant de Amêndoas, Brownie com Sorvete e Torta de Frutas Silvestres. Não existe marca de procedência que permita distinguir seed antigo de edição administrativa. **Nenhum desses registros foi sobrescrito.** O seed continua exclusivo de dev e só atua com ambas as tabelas vazias.

Correção administrativa controlada: consultar `GET /api/admin/produtos/{id}`, conferir nome/descrição e fotografia, e enviar `PUT /api/admin/produtos/{id}` com o estado atual e somente a imagem intencionalmente alterada. Não copiar um JSON antigo nem reativar itens. Exemplo para Espresso após conferir o cadastro:

```json
{
  "nome": "Espresso Italiano",
  "descricao": "Descrição atual conferida no GET",
  "preco": 10.00,
  "imagemUrl": "/img/cupEsquerdaCima.jpg",
  "categoriaId": 1,
  "ativo": true,
  "disponivel": true
}
```

Os demais campos acima são ilustrativos: preserve os valores reais do GET, inclusive flags. Brownie pode usar `cardapioComida.jpg`; não foi inferida automaticamente uma foto para cold brew, soda ou croissant de amêndoas. Imagens históricas dos pedidos novos não mudam quando o cadastro é editado depois.

## 4. Arquitetura e padrões

`Thymeleaf/JavaScript → PedidoController → PedidoService → Repositories → Pedido/ItemPedido → DTO de resposta`.

MVC separa interação HTTP e apresentação. SRP mantém cálculos/consultas fora do controller e separa pedidos do CRUD de produtos. Repository Pattern reutiliza Spring Data JPA. DTOs validam entradas e evitam exposição de entidades/proxies/contatos. Injeção por construtor declara dependências. Encapsulamento mantém a lista imutável para consumidores e sincroniza os dois lados do relacionamento no método `adicionarItem`.

`TransactionTemplate` delimita criação completa e permite recuperar uma corrida de idempotência **depois** do rollback da transação que perdeu a disputa. As consultas de acompanhamento usam `@Transactional(readOnly=true)`. Uma consulta em lote carrega produtos/categorias; não há SELECT por item. Não foram criadas interfaces, factories ou hierarquias artificiais.

## 5. Fluxo e regras

1. O checkout envia IDs/quantidades/tipo para cotação com CSRF.
2. O serviço valida entre 1 e 50 produtos distintos, 1–99 unidades por produto e até 200 unidades totais, existência, atividade e disponibilidade, inclusive categoria ativa.
3. Preços oficiais vêm do H2, com `BigDecimal`. `cafe.pedido.taxa-entrega=7.00` configura entrega; retirada custa zero. O valor total respeita a precisão monetária do esquema.
4. O cliente visualiza os valores oficiais e confirma com token de cotação e chave de idempotência. Não envia preço oficial, total ou status.
5. O serviço repete a validação no banco e compara o resumo com a cotação. Mudança de preço/condições, tipo, quantidade ou expiração gera 409, sem gravar o pedido. O frontend apresenta o novo resumo e exige outra confirmação explícita.
6. Pedido e itens são persistidos atomicamente; nomes/preços/imagens são snapshots. Status inicial `RECEBIDO` e pagamento `PENDENTE` são definidos pelo servidor.
7. Somente após HTTP 201 válido a sacola é limpa e ocorre o redirecionamento.
8. A confirmação consulta os valores persistidos, também após reload/reinício. Não existem produtos/status demonstrativos como fallback.

Campos de contato são obrigatórios (telefone normalizado para 10–11 dígitos, e-mail válido); endereço é obrigatório para entrega, opcional para retirada. CEP tem oito dígitos. Observações são opcionais, até 500 caracteres. O endereço é formatado na coluna existente; não foi introduzido um objeto JPA desnecessário.

## 6. Segurança, concorrência e privacidade

CSRF permanece ativo para cotação e criação. O token fornecido pelo Spring/Thymeleaf é enviado no cabeçalho indicado por `_csrf_header`. Sem token, POST retorna 403. A exceção administrativa existente não foi ampliada; HTTP Basic/ROLE_ADMIN continuam protegendo o CRUD, e o console H2 continua bloqueado.

Não existem endpoints públicos de listagem por ID sequencial nem alteração de status. A referência de consulta possui 256 bits gerados por `SecureRandom`, Base64 URL-safe. O ID numérico é apenas identificador visual; não autoriza consulta. A referência é uma credencial de acesso: não compartilhe o link. A URL da confirmação usa fragmento, evitando enviá-la na requisição da página. Respostas têm `Cache-Control: no-store` e há política `no-referrer`.

A consulta não retorna nome, telefone, e-mail nem observações. Retorna o endereço somente porque ele é necessário ao resumo solicitado e apenas mediante referência válida. O token público é armazenado no banco para permitir a mesma resposta em reenvios idempotentes; foi preferida essa opção ao hash irrecuperável para não perder a consulta após uma resposta de criação perdida. Cotações e chaves de idempotência são armazenadas somente como hashes. Não há dump de requisições/contatos em logs.

O banco impõe unicidade de idempotência/cotação. Reenvio idêntico retorna o mesmo pedido (201), inclusive depois de reiniciar ou alterar preço/disponibilidade. Mesma chave com outro payload gera 409. Requisições concorrentes são testadas. A store guarda, por sessão, somente chave/fingerprint e cotação (sem contato/endereço) para retomar falhas; não usa apenas o estado do botão para impedir duplicação.

JSON desconhecido ou campos como `total`, `status`, `statusPagamento`, `precoUnitario` são rejeitados com 400, inclusive em DTOs aninhados. Erros não expõem SQL/stack traces/detalhes do H2. Falha inesperada de persistência retorna 500 e reverte tudo.

## 7. Contratos HTTP

### Cotação: `POST /api/pedidos/cotacao` → 200

Enviar CSRF e `Content-Type: application/json`:

```json
{"tipoAtendimento":"ENTREGA","itens":[{"produtoId":1,"quantidade":2}]}
```

Resposta contém `cotacao` (token), `expiraEm`, itens oficiais com preços/subtotais/imagem, subtotal, taxaEntrega e total. Valores e tokens dos exemplos devem ser obtidos da sua instância.

### Criação: `POST /api/pedidos` → 201

Cabeçalhos: CSRF e `Idempotency-Key` com 32–100 caracteres `[A-Za-z0-9_-]` (ex.: UUID gerado uma vez por tentativa lógica).

```json
{
  "nomeCliente":"Cliente Exemplo",
  "telefoneCliente":"11999999999",
  "emailCliente":"cliente@example.com",
  "tipoAtendimento":"ENTREGA",
  "formaPagamento":"DINHEIRO",
  "endereco":{"cep":"01310000","logradouro":"Av Paulista","numero":"100","complemento":"","bairro":"Bela Vista"},
  "observacoes":"Sem açúcar",
  "itens":[{"produtoId":1,"quantidade":2}],
  "cotacao":"TOKEN_REAL_RETORNADO_PELA_COTACAO"
}
```

Para retirada: `tipoAtendimento=RETIRADA`, `endereco=null`. Formas: `PIX`, `CARTAO_CREDITO`, `CARTAO_DEBITO`, `DINHEIRO`; a opção visual “Cartão (crédito)” mapeia explicitamente para `CARTAO_CREDITO`. Nenhuma forma é processada financeiramente nesta etapa.

Resposta: `id`, `referencia`, `status`, `criadoEm`, `tipoAtendimento`, `formaPagamento`, `statusPagamento`, itens históricos (`produtoId`, `nomeProduto`, `quantidade`, `precoUnitario`, `subtotal`, `imagemUrl`), subtotal, taxaEntrega, total e enderecoEntrega. `Location` aponta para a consulta segura.

### Consulta: `GET /api/pedidos/consulta/{referencia}` → 200

404 para referência inválida/inexistente. Não há `GET /api/pedidos/{id}` nem endpoint público para alterar status. Erros: 400 para entrada/JSON inválido, 403 para CSRF ausente/inválido, 404 produto inexistente, 409 indisponibilidade/categoria inativa/preço ou cotação alterados/idempotência conflitante, 500 falha inesperada sem detalhes internos.

## 8. Testes e evidências

O comando de regressão é `./mvnw clean test`. No ambiente da nuvem foi executado com as opções de cache/proxy do onboarding e JDK 21:

```bash
export JAVA_HOME=/workspace/.aurora-setup/jdk-21
export MAVEN_USER_HOME=/workspace/.m2
export MAVEN_OPTS=-Djavax.net.ssl.trustStore=/etc/ssl/certs/java/cacerts
./mvnw -s /workspace/.aurora-setup/settings.xml -Dmaven.repo.local=/workspace/.m2/repository clean test package
```

**52 testes Java aprovados, zero falhas/erros/ignorados**, incluindo os 21 anteriores. Os 31 novos testes abrangem domínio, entrega/retirada, valores exatos/históricos, validações, duplicados, disponibilidade/categoria, mudança de preço, validade de cotação, rollback real (falha induzida na tabela de itens), unicidade concorrente, CSRF, JSON extra, privacidade e tratamento seguro de 500.

`node scripts/test-pedidos.cjs` executa o fluxo no Chromium: dois Espressos + um Croissant → sacola → formulário → HTTP 201 → detalhes/reload, dinheiro pendente e avaliação bloqueada; repete para retirada, altera disponibilidade/preço, perde uma resposta após o servidor aceitar e reenvia com a mesma chave. Após parar o servidor confere `pedidos` e `itens_pedido` por JDBC e compara valores; reinicia e confere consulta e idempotência. `LEGACY_H2_FILE` opcional é copiado para testar atualização de esquema com um registro legado. O original nunca é modificado pelo script.

O script usa uma pasta temporária informada ao final e precisa de Java 21, JAR empacotado, módulo Playwright e Chromium (`CHROMIUM_PATH` opcional). Os dados de cliente são fictícios e a senha administrativa é gerada somente em memória. A regressão de navegador da Etapa 1 está em `scripts/test-cardapios.cjs`.

## 9. Execução no Codespaces

```bash
java -version  # confirmar JDK 21, incluindo javac
./mvnw clean test
read -rsp 'Senha administrativa local: ' CAFE_ADMIN_PASSWORD
export CAFE_ADMIN_PASSWORD
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

Abrir a porta 8080 pelo painel de portas do Codespaces. Manter a porta privada durante desenvolvimento. H2 fica em `data/`, que já é ignorado pelo Git. Antes de atualizar um banco importante, fazer backup com a aplicação parada. Não apagar `data/` para aplicar a etapa. Para mudar a taxa, configurar `cafe.pedido.taxa-entrega` (ex.: argumento `--cafe.pedido.taxa-entrega=9.00` ao iniciar o JAR); checkout e sacola recebem o valor do servidor.

Para testes de navegador, instalar Playwright/Chromium apenas no ambiente de testes, sem alterar `pom.xml` nem declarações de dependências do aplicativo. Exportar `JAVA_HOME` e, se necessário, `MAVEN_USER_HOME` e `CHROMIUM_PATH`. Os scripts não abrem o H2 de desenvolvimento como banco de teste.

## 10. Limites das próximas etapas

Não há gateway, cobrança PIX, QR fictício, coleta de cartão, alteração administrativa de status, envio real de avaliação/foto, autenticação de clientes, gestão de estoque ou notificação à cafeteria. O status permanece RECEBIDO/PENDENTE até existir fluxo administrativo próprio. O formulário visual de avaliação e suas transições permanecem, mas o botão não é liberado enquanto o pedido real não estiver concluído.

Cotações vencidas permanecem no banco para histórico técnico; política operacional de retenção/limpeza, limitação de tráfego, gestão de dados pessoais e banco de produção devem ser definidos antes de publicação comercial. Não houve comparação automatizada de pixels; CSS e componentes existentes foram preservados e o fluxo visual foi exercitado no navegador.

Evidências finais desta execução: `/tmp/etapa2-final-tests.log` (52 testes aprovados), `/tmp/etapa2-e2e-final.log` e `/tmp/aurora-pedidos-C5PhJ3` (fluxo real, snapshot dos itens, perda de resposta seguida de reload e idempotência, conferência JDBC, reinício e registros legados); `/tmp/etapa2-regressao-browser.log` e `/tmp/aurora-functional-f5LjhC` (regressão funcional da Etapa 1). A falha encontrada no teste de retomada após reload foi corrigida preservando a chave recuperada durante a inicialização da store; o cenário completo foi repetido com sucesso. Os servidores de teste foram encerrados.
