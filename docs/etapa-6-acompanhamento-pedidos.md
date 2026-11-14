# Etapa 6 — acompanhamento automático e histórico de pedidos

## Escopo e rastreabilidade

Branch: `etapa-6-acompanhamento-pedidos`. Commit inicial e main remota conferida por `git ls-remote origin refs/heads/main`: `ebe7931f04593a67f001c8a06294080d1a66fd71`. Árvore inicialmente limpa. Sem merge e sem PR. Commit de implementação e testes: `460c559e1b068c2d0376128b3141ba887bde82b0`, publicado na branch de trabalho e confirmado por `git ls-remote origin refs/heads/etapa-6-acompanhamento-pedidos`. Este relatório integra um commit documental posterior; seu SHA final é informado na entrega, pois não é possível inserir o hash de um commit no próprio conteúdo. A confirmação remota final é feita após esse commit documental. O aviso automático do GitHub sugerindo criar PR não representa criação de PR.

Foram inspecionados os enums, entidade Pedido, repositório de pedidos, PedidoService, PedidoResponse, PedidoController, AdminPedidoService, AdminPainelController, AvaliacaoService, os templates de acompanhamento e detalhes administrativos, JavaScript de acompanhamento e avaliações, CSS administrativo, configurações H2 e suítes Java/Playwright anteriores. Nenhum pagamento, preço, taxa, checkout, cotação, catálogo, autenticação ou upload foi redesenhado.

## Regras de domínio

`Pedido.proximosStatus()` é a matriz única de autorização, utilizada por `alterarStatus()` e pelo formulário administrativo. Não há comparação ordinal de enums.

| Tipo | Estado | Próximos estados |
|---|---|---|
| ENTREGA | RECEBIDO | EM_PREPARO, CANCELADO |
| ENTREGA | EM_PREPARO | PRONTO, CANCELADO |
| ENTREGA | PRONTO | EM_ENTREGA, CANCELADO |
| ENTREGA | EM_ENTREGA | CONCLUIDO |
| ENTREGA | CONCLUIDO | Nenhum |
| ENTREGA | CANCELADO | Nenhum |
| RETIRADA | RECEBIDO | EM_PREPARO, CANCELADO |
| RETIRADA | EM_PREPARO | PRONTO, CANCELADO |
| RETIRADA | PRONTO | CONCLUIDO, CANCELADO |
| RETIRADA | EM_ENTREGA | Estado proibido, nenhuma saída |
| RETIRADA | CONCLUIDO | Nenhum |
| RETIRADA | CANCELADO | Nenhum |

São rejeitados null, repetição, saltos, regressões e alterações de estados terminais. Status desconhecido é rejeitado pelo binding HTTP. Retirada nunca pode ingressar em EM_ENTREGA. Conclusão significa entrega/retirada efetiva. Cancelamento comum termina em PRONTO; não é permitido após a saída para entrega. Nenhuma alteração de pagamento acompanha essas operações.

## Histórico, concorrência e transações

Tabela aditiva `historico_status_pedido`, com ID gerado, FK obrigatória ao pedido, anterior anulável, novo obrigatório, `Instant alteradoEm`, responsável e origem (`SISTEMA`/`ADMINISTRADOR`). Colunas não atualizáveis e ausência de setters. Repositório restrito a inserção e consulta ordenada por instante e ID; não existem endpoints de edição/exclusão de eventos.

`HistoricoStatusPedidoService.registrar` exige transação existente (`MANDATORY`). PedidoService insere o evento inicial após persistir o pedido e antes do commit, na mesma TransactionTemplate. Anterior null, novo RECEBIDO, responsável Sistema e instante igual ao instante real de criação do pedido. Repetição idempotente retorna o pedido existente sem inserir outro evento. Falha de inserção também reverte a criação.

AdminPedidoService obtém `PESSIMISTIC_WRITE` no pedido por ID, lê o estado sob esse bloqueio, aplica a transição no domínio e grava o evento na mesma transação. Duas decisões incompatíveis a partir de PRONTO não podem sobrescrever uma à outra: a segunda operação valida o estado já confirmado pela primeira. Falhas de lock são tratadas com mensagem controlada no painel. Falha ao salvar histórico reverte o status. O responsável é o nome do Authentication atual; chamadas internas sem identidade usam Administração. Não são armazenados tokens, senhas ou cookies.

Somente o DTO administrativo expõe eventos, sem carregar informações pessoais adicionais. O controller mantém as rotas administrativas protegidas e CSRF. A consulta pública e seu `Cache-Control: no-store` permanecem intactos; não contêm histórico ou identificação de funcionários.

A apresentação utiliza America/Bahia, enquanto a persistência utiliza Instant/UTC. O painel mantém dados pessoais, itens, valores, mensagens e formulário, com ações calculadas pelo domínio, rótulos de entrega/retirada e uma lista cronológica vertical, sem tabela larga. Pedidos legados sem eventos exibem histórico indisponível; nenhum evento ou horário retroativo é inventado.

## Acompanhamento e polling

Quatro posições preservadas. Entrega: recebido (0), preparo (1), motocicleta (2), casa/entregue (3). PRONTO permanece na posição 1 com texto de espera pela saída; somente EM_ENTREGA ativa a motocicleta como etapa atual. Em CONCLUIDO a casa é atual e as anteriores são concluídas. Cancelamento remove o progresso ativo.

Retirada: recebido (0), preparo (1), pronto para retirada (2, ícone de confirmação existente), retirado (3). Não utiliza motocicleta. Textos e rótulos acessíveis dependem do tipo. Nenhum arquivo de CSS do acompanhamento, xícara, menu ou animações de avaliação foi alterado.

A consulta é imediata ao abrir uma referência válida de 43 caracteres no fragmento. Um único timeout agenda a próxima leitura 15 segundos após a consulta anterior. Há uma única Promise em andamento, compartilhada por refresh simultâneos; timeout de rede de 10 segundos e AbortController. Não há setInterval nem reload.

Aba oculta cancela o agendamento; o retorno visível consulta imediatamente e retoma o ciclo. Pagehide elimina timer e aborta a requisição; pageshow de BFCache reativa o fluxo. CONCLUIDO/CANCELADO param as consultas periódicas, sem impedir refresh explícito usado pela avaliação. Referência inválida não gera consulta; 400/404/410 encerram retentativas. Erros de rede/HTTP/JSON preservam o último pedido e exibem aviso discreto; backoff de 30/60 segundos e recuperação removem o aviso. A referência não vai para logs adicionais ou terceiros.

Resposta sem mudança relevante não renderiza novamente. Somente a primeira resposta constrói o resumo; alterações de status/tipo/jaAvaliado atualizam o acompanhamento sem fechar painéis, mexer no foco ou limpar campos/foto. Uma avaliação já confirmada não regride por resposta antiga. Os contratos `window.CafeAuroraPedido`, getStatus, jaAvaliado, refresh e eventos cafeaurora:review-status/open-review foram preservados.

## Integração com avaliações e dados

O botão só habilita em CONCLUIDO e sem avaliação existente. AvaliacaoService continua sendo a autorização definitiva. EM_ENTREGA permanece inelegível. Persistência, moderação, consentimento, foto e carrossel permanecem na implementação da Etapa 5.

Todos os pedidos demonstrativos e demais dados foram mantidos. Não houve limpeza nem alteração de ddl-auto. Testes Java utilizam bancos em memória separados; compatibilidade usa arquivo temporário. E2E utiliza bancos temporários, portas 18081–18084 e senhas aleatórias não impressas. A limpeza dos testes agora remove explicitamente eventos antes de excluir seus pedidos fictícios, respeitando a nova FK; inclusive o teste de exclusão concorrente durante leitura de imagem. Nada aponta para o H2 persistente de desenvolvimento.

## Validação e evidências

Resultado: **APROVADA PARA REVISÃO**.

| Grupo | Executados | Aprovados | Falhas | Ignorados |
|---|---:|---:|---:|---:|
| Java unitários (domínio, DTO e services) | 86 | 86 | 0 | 0 |
| Java integração | 204 | 204 | 0 | 0 |
| Playwright (scripts completos) | 4 | 4 | 0 | 0 |

A suíte completa final `test` executou 290 casos (baseline anterior: 164); `verify` também passou antes da ampliação final da matriz administrativa, com 254 casos e geração do JAR. As alterações posteriores são somente testes/documentação; o código de produção do JAR é o validado. O teste de concorrência foi refinado para atribuir duas identidades administrativas distintas às threads e conferir o ator vencedor, seguido de reexecução da classe.

Evidências finais nesta máquina:

- `/tmp/etapa6-final-tests.log`: 290 testes, 0 falhas/erros/ignorados, BUILD SUCCESS.
- `/tmp/etapa6-verify.log`: verify/empacotamento aprovado.
- `/tmp/etapa6-concorrencia.log`: reexecução dos cinco testes de histórico com identidades distintas.
- `/tmp/etapa6-acompanhamento.log`: 65 verificações, execução completa; capturas em `/tmp/aurora-acompanhamento-ZO94MK`, nas larguras 1366, 768 e 390. Inclui campos/foto/foco preservados, um único timer após refreshes repetidos, rejeição de reabertura e contagens de histórico para ambos os atendimentos e cancelamento.
- `/tmp/etapa6-avaliacoes.log`: script original completo aprovado; `/tmp/aurora-avaliacoes-RYmnsR`.
- `/tmp/etapa6-pedidos.log`: fluxo anterior completo aprovado; `/tmp/aurora-pedidos-vrLEKt`.
- `/tmp/etapa6-cardapios.log`: categorias CAFE/DRINKS/COMIDAS, sacola e reinício aprovados; `/tmp/aurora-functional-Qe1D8Z`.

As primeiras execuções de navegador incluíram timeout de startup sob concorrência e uma asserção do runner anterior de avaliações que não se repetiu nas duas execuções posteriores (uma diagnóstica e outra do script original). Foi utilizado `JAVA_TOOL_OPTIONS=-XX:TieredStopAtLevel=1` nas repetições para reduzir custo de startup/JIT; não altera regras ou assertions. O teste novo inicialmente navegava apenas pelo fragmento após criar o pedido, sem executar novamente a página; foi corrigido para uma navegação completa com query de teste sem referência. Nenhuma falha dessas tentativas é contabilizada como aprovação; a tabela acima descreve as execuções finais completas. Uma captura mobile foi inspecionada visualmente, além das assertions de viewport.

Comandos desta máquina:

```sh
bash /workspace/.aurora/maven.sh verify
bash /workspace/.aurora/maven.sh test
JAVA_TOOL_OPTIONS=-XX:TieredStopAtLevel=1 JAVA_HOME=/workspace/.aurora/jdk-root/usr/lib/jvm/java-21-openjdk-amd64 MAVEN_USER_HOME=/workspace/.m2 node scripts/test-acompanhamento.cjs
# Mesmas variáveis para test-avaliacoes.cjs, test-pedidos.cjs e test-cardapios.cjs.
```

O helper externo usa JDK 21 completo, proxy Maven com TLS e cache gravável; não faz parte do código do projeto. Relatórios JUnit atuais ficam em target/surefire-reports (ignorado pelo Git). Logs de trabalho em /tmp/etapa6-*.log.

Novos testes: matriz de 72 pares de domínio; matriz administrativa de 72 pares para ambos os tipos, com contagem de eventos e ausência de histórico público; histórico inicial e idempotência; instantes, responsáveis e ordem; legado vazio; rollback de criação e de atualização; disputa real em duas threads. O E2E cobre fluxos completos, persistência após reinício, avaliação de entrega, falhas/recovery, visibilidade, timers, não sobreposição, referências e larguras desktop/tablet/mobile. Os scripts anteriores são preservados e reexecutados.

Expectativas antigas legitimamente alteradas: EM_ENTREGA agora é filtro válido; quantidade de enums nos indicadores; entrega passa por EM_ENTREGA antes da avaliação. A retirada mantém o caminho PRONTO → CONCLUIDO. Primeiras execuções expuseram essas expectativas e a necessidade de limpeza da FK nos testes; os resultados finais substituem essas falhas de desenvolvimento, sem omiti-las como diagnóstico.

## Arquivos e responsabilidades

- StatusPedido, OrigemStatusPedido e Pedido: novo estado, origens e matriz de domínio.
- HistoricoStatusPedido e seu repositório/service: persistência e DTO administrativo.
- PedidoRepository: leitura com lock pessimista.
- PedidoService: evento inicial transacional/idempotente.
- AdminPedidoService: evento administrativo, ator e lock.
- AdminPainelController: carregamento do histórico e conflito de lock.
- admin/pedido.html e admin-order-detail.css: histórico vertical e ações claras.
- pedido-confirmado.js: estados e polling, mantendo contratos da avaliação.
- FluxoStatusTest, HistoricoStatusTests, AdminIntegracaoTests: nova cobertura.
- AvaliacaoIntegracaoTests, PedidoIntegracaoTests: limpeza isolada e fluxo de entrega atualizado.
- scripts/test-acompanhamento.cjs: E2E da Etapa 6.
- Este relatório: arquitetura, resultados e rastreabilidade.

## Limites e próximas etapas

Não há tratamento operacional de falha após saída para entrega, GPS, entregadores, notificações ou pagamentos. Não foram adicionados. Histórico legado permanece desconhecido. O usuário administrativo continua configurado por ambiente, sem cadastro de funcionários. H2/ddl-auto=update seguem voltados ao desenvolvimento; migrações e banco de produção ficam para uma etapa própria. Polling é aproximadamente 15 segundos mais a duração da requisição e depende das políticas de timers do navegador. Sem alterações de identidade visual; capturas responsivas são evidência complementar, não aprovação humana do design.

## Revisão final — ícone principal de PRONTO e verify completo

Correção pontual na mesma branch `etapa-6-acompanhamento-pedidos`, a partir de `05e746a44ec3db2a812eea62fa74a72a85452851` (HEAD local e remoto conferidos antes da alteração). Esta seção registra a validação da revisão e substitui, para essa revisão, os resultados históricos de build acima.

`applyStatus()` agora usa o SVG de confirmação já existente (`ICONS.RECEBIDO`) para o ícone principal de PRONTO, em vez do chapéu de preparo. O check representa preparação concluída. Em retirada, permanece o texto exato “Pronto para retirada”; em entrega, permanece a indicação de preparo encerrado aguardando saída. O mapeamento das quatro posições e a barra não mudaram: ENTREGA + PRONTO mantém progresso 1/3 e motocicleta inativa, e RETIRADA + PRONTO utiliza progresso 2/3. A motocicleta só se torna a etapa atual em EM_ENTREGA.

O E2E `scripts/test-acompanhamento.cjs` passou a verificar, nos dois atendimentos, a visibilidade e a geometria do check principal, distinguindo-o do ícone de preparo e da motocicleta, além das quatro posições. Também verifica o texto exato da retirada, o progresso 1/3 e 2/3 e que a motocicleta em entrega PRONTO não está atual nem concluída. São geradas capturas `pronto-entrega.png` e `pronto-retirada.png`.

Escopo da revisão: somente `pedido-confirmado.js`, `test-acompanhamento.cjs` e este relatório. Não houve alteração de CSS, fontes, cores, animações, regras de status, pagamentos ou avaliações. Nenhum banco de desenvolvimento foi apagado; todos os E2E utilizam H2 e credenciais temporários próprios.

Comandos de validação desta revisão:

```sh
bash /workspace/.aurora/maven.sh verify > /tmp/etapa6-revisao-verify.log 2>&1
# Para cada nome: acompanhamento, avaliacoes, pedidos, cardapios
JAVA_TOOL_OPTIONS=-XX:TieredStopAtLevel=1 \
JAVA_HOME=/workspace/.aurora/jdk-root/usr/lib/jvm/java-21-openjdk-amd64 \
MAVEN_USER_HOME=/workspace/.m2 \
node scripts/test-<nome>.cjs > /tmp/etapa6-revisao-<nome>.log 2>&1
```

O `verify` desta revisão executou **290 testes**, com **0 falhas, 0 erros e 0 ignorados**, gerou o JAR atualizado e terminou com `BUILD SUCCESS` em 59,684 segundos. Assim, o build completo foi executado com a matriz final e os testes de concorrência atualizados, e não apenas presumido a partir da execução anterior de `test`.

Resultados de navegador desta revisão (todos com o JAR produzido pelo verify acima):

| Script | Resultado real | Log e evidências |
|---|---|---|
| `test-acompanhamento.cjs` | Aprovado, 76 verificações, saída 0 | `/tmp/etapa6-revisao-acompanhamento.log`; `/tmp/aurora-acompanhamento-eznJVg` |
| `test-pedidos.cjs` | Aprovado, saída 0 | `/tmp/etapa6-revisao-pedidos.log`; `/tmp/aurora-pedidos-cPdi8A` |
| `test-cardapios.cjs` | Aprovado, saída 0 | `/tmp/etapa6-revisao-cardapios.log`; `/tmp/aurora-functional-xGxpqQ` |
| `test-avaliacoes.cjs` | Primeira tentativa: falha de asserção no carrossel, saída 1; repetição completa: aprovada, saída 0 | `/tmp/etapa6-revisao-avaliacoes.log`; `/tmp/etapa6-revisao-avaliacoes-repeticao.log`; `/tmp/aurora-avaliacoes-1KohtR` |

A repetição de avaliações usou o script original, com as mesmas assertions, em nova instância/banco temporários e com menor concorrência de processos de validação. A intermitência do carrossel já havia sido observada na entrega inicial; sua causa não foi confirmada. Não foi tratada como aprovação da primeira tentativa, nem foram alterados pagamentos, avaliações ou o runner anterior para contorná-la. Recomenda-se investigar a estabilidade desse E2E separadamente.

As capturas `pronto-entrega.png` e `pronto-retirada.png` foram inspecionadas visualmente: ambas exibem check principal; entrega mantém a motocicleta inativa e retirada mantém a terceira posição de confirmação. O cenário ampliado continua verificando polling, reinício, avaliação e larguras de desktop/tablet/mobile. `node --check` passou nos dois JavaScripts alterados, e `git diff --check` passou.

A correção é publicada exclusivamente na branch da Etapa 6, sem merge ou PR. O SHA do commit desta revisão é informado na entrega e pode ser conferido por `git rev-parse HEAD` e `git ls-remote origin refs/heads/etapa-6-acompanhamento-pedidos`; não se inclui o hash do próprio commit no seu conteúdo.


## Investigação de estabilidade do carrossel — após a revisão do ícone

Base: `42025bf21f9878320aaf7d861f98065c6a0c8c8b`, branch `etapa-6-acompanhamento-pedidos`, árvore inicialmente limpa. A correção aprovada de PRONTO não foi refeita. O escopo desta investigação é o runner de avaliações e a evidência de estabilidade; nenhuma regra de pagamento, status ou histórico foi alterada.

### Evidência original e limites do diagnóstico

`/tmp/etapa6-revisao-avaliacoes.log` ainda existe. Ele contém sucessos de carrossel para 0, 1, 2, 3 e 4 avaliações, seguidos de `AssertionError [ERR_ASSERTION]: Expected values to be strictly equal:`. O catch antigo imprimia somente a primeira linha da exceção; portanto a linha exata, o valor esperado e o observado não foram preservados. Não é possível recuperar esses detalhes desse log, e não se atribui a falha a uma asserção específica sem evidência.

A fixture temporária correspondente, `/tmp/aurora-avaliacoes-3C1bQt`, contém as capturas até `carousel-4-mobile.png` e cinco avaliações APROVADA. A contagem foi consultada com H2 `ACCESS_MODE_DATA=r;IFEXISTS=TRUE`, exclusivamente no banco de teste já encerrado. Resultado em `/tmp/aurora-estabilidade/banco-falha-original.log`. Com isso, a falha é localizada no cenário de carrossel com cinco avaliações, após a quinta aprovação; o subpasso exato do erro histórico permanece desconhecido.

A inspeção de scripts.js encontrou atualização síncrona das classes dos cards e dos indicadores, sem temporizador que avance automaticamente o carrossel. CSS possui transição de largura dos indicadores (0,3 s) e entrada do conjunto (1,2 s com atraso de 0,4 s). O runner faz verificações imediatas após os cliques. Isso torna sincronização/animação uma hipótese plausível, mas a inspeção por si só não prova a causa nem exclui um defeito real ou efeito de concorrência. Não foram removidas animações ou adicionados sleeps/repetições para fazer o teste passar.

### Cinco execuções completas, sequenciais e independentes

Comando de cada execução:

```sh
JAVA_TOOL_OPTIONS=-XX:TieredStopAtLevel=1 \
JAVA_HOME=/workspace/.aurora/jdk-root/usr/lib/jvm/java-21-openjdk-amd64 \
MAVEN_USER_HOME=/workspace/.m2 \
node scripts/test-avaliacoes.cjs
```

Cada execução criou um novo diretório por mkdtemp, copiou o JAR, utilizou H2 de arquivo temporário próprio, senha aleatória e porta 18083. O processo anterior foi encerrado antes de iniciar o próximo. Nenhum outro E2E foi lançado durante essa série. O runner continuou exercitando 0 a 7 avaliações cadastradas (até cinco públicas), setas, todos os indicadores, mobile, fotos, moderação, falha de envio, resposta perdida, limites HTTP, duplicidade e persistência após reinício. A opção reducedMotion já existente foi preservada; nenhum CSS foi modificado.

Os cinco testes foram planejados como série finita, e não como retentativas até obter sucesso. Todos os códigos de saída foram armazenados, inclusive se fossem não zero. Tempos abaixo em America/Sao_Paulo, em 14/11/2026; o TSV bruto utiliza UTC.

| Execução | Início | Fim | Saída | Resultado | Evidências |
|---|---|---|---:|---|---|
| 1 | 19:05:49 | 19:06:59 | 0 | Aprovada | `/tmp/aurora-estabilidade/antes-1.log`; `/tmp/aurora-avaliacoes-togWwD` |
| 2 | 19:06:59 | 19:08:02 | 0 | Aprovada | `/tmp/aurora-estabilidade/antes-2.log`; `/tmp/aurora-avaliacoes-nvIDjS` |
| 3 | 19:08:02 | 19:09:06 | 0 | Aprovada | `/tmp/aurora-estabilidade/antes-3.log`; `/tmp/aurora-avaliacoes-Wprc8H` |
| 4 | 19:09:06 | 19:10:09 | 0 | Aprovada | `/tmp/aurora-estabilidade/antes-4.log`; `/tmp/aurora-avaliacoes-LjDQfu` |
| 5 | 19:10:09 | 19:11:15 | 0 | Aprovada | `/tmp/aurora-estabilidade/antes-5.log`; `/tmp/aurora-avaliacoes-zaQNse` |

Ressalva metodológica: essa primeira série já continha logging de cenário e uma leitura de atributo para identificar o indicador antes do clique. Nenhuma assertion foi removida ou relaxada. A leitura adicional foi posteriormente substituída pelo índice local da iteração, para que a instrumentação final não acrescente uma chamada assíncrona antes de cada clique e não interfira desnecessariamente no timing observado.

### Recorte focado: cinco avaliações, cliques e animações originais

Foram feitas mais 20 navegações controladas com a fixture de cinco avaliações da falha histórica, copiada para outro diretório temporário e servida na porta 18085. O helper de investigação `/workspace/.aurora/focus-carousel.cjs` preservou as verificações originais de contagem, centro, seta próxima/anterior e cada indicador, com viewport 1366 × 900 e animações originais. Não adicionou leitura de atributo antes dos cliques; estava preparado para registrar pointerdown, pointerup e click, estado do DOM e animações se uma asserção falhasse.

Resultado: **20 aprovadas, 0 falhas**; cada tentativa está registrada individualmente em `/tmp/aurora-estabilidade/foco-original.log`. Diretório: `/tmp/aurora-carousel-focus-SdrSHt`. Como não houve recorrência, não existe dump de clique falho que permita comparar o alvo do mouse com o indicador esperado. O recorte não substitui o E2E completo: utiliza dados previamente preparados e não testa novamente criação/upload/moderação em cada navegação.

### Alteração necessária no runner: preservar diagnósticos

O único código versionado alterado nesta investigação é `scripts/test-avaliacoes.cjs`:

- Identifica cenário, quantidade de avaliações e subpasso (contagens, próxima/anterior, indicador, mobile).
- Em falha no carrossel, captura índices e ordem dos cards, centro, indicadores ativos, largura/scroll e screenshot.
- Preserva a exceção original mesmo se a captura auxiliar falhar.
- Imprime stack trace completo com caminho das evidências, em vez de truncar a mensagem na primeira linha. Senha e referências longas são redigidas.
- Não acrescenta esperas, sleeps, novos cliques ou retentativas. O índice local identifica o indicador sem chamada assíncrona extra. Todas as 33 chamadas estáticas de assert anteriores permanecem; a contagem estática não é apresentada como quantidade de assertions executadas em tempo de execução.

A captura detalhada do DOM e a screenshot adicional só acontecem após uma falha. Layout, cards Polaroid, animações, indicadores, setas e lógica da aplicação permanecem intactos. Não se apresenta essa melhoria de diagnóstico como correção da causa da intermitência.

### Build e validação da versão final do runner

`bash /workspace/.aurora/maven.sh verify` foi executado novamente: **290 testes, 0 falhas, 0 erros, 0 ignorados**, JAR gerado, `BUILD SUCCESS`, duração de 57,820 segundos. Log: `/tmp/aurora-estabilidade/verify.log`. `node --check scripts/test-avaliacoes.cjs` e `git diff --check` também passaram.

Após esse build, iniciou-se outra série fixa de cinco E2E completos, sequenciais, com a instrumentação final sem leitura extra antes do clique. Essa série valida o runner que será publicado; não é uma repetição condicional para mascarar falhas. Cada execução mantém seu próprio H2 temporário, credenciais e log, e o resultado de cada uma é registrado em `/tmp/aurora-estabilidade/final.tsv`.

Resultados individuais da série final (America/Sao_Paulo, 14/11/2026):

| Execução | Início | Fim | Saída | Resultado | Evidências |
|---|---|---|---:|---|---|
| 1 | 19:14:18 | 19:15:18 | 0 | Aprovada | `/tmp/aurora-estabilidade/final-1.log`; `/tmp/aurora-avaliacoes-9MjbKW` |
| 2 | 19:15:18 | 19:16:19 | 0 | Aprovada | `/tmp/aurora-estabilidade/final-2.log`; `/tmp/aurora-avaliacoes-hC64uR` |
| 3 | 19:16:19 | 19:17:19 | 0 | Aprovada | `/tmp/aurora-estabilidade/final-3.log`; `/tmp/aurora-avaliacoes-ZkcbyL` |
| 4 | 19:17:19 | 19:18:22 | 0 | Aprovada | `/tmp/aurora-estabilidade/final-4.log`; `/tmp/aurora-avaliacoes-y1R8Kc` |
| 5 | 19:18:22 | 19:19:24 | 0 | Aprovada | `/tmp/aurora-estabilidade/final-5.log`; `/tmp/aurora-avaliacoes-UoO20O` |

### Conclusão e publicação

**Falha não reproduzida; causa indeterminada.** Resultado desta investigação: 10 execuções completas aprovadas (duas séries sequenciais de cinco, cada execução com banco independente), 20 navegações focadas aprovadas e verify com 290 testes aprovados, sem falhas, erros ou ignorados. Cada série foi concluída e registrada integralmente; não houve execução repetida até esconder uma falha.

A falha histórica foi localizada no cenário com cinco avaliações, mas sua asserção exata não pode ser recuperada do log truncado. Não houve recorrência com stack trace e contexto disponíveis. Assim, não é possível concluir causalmente se foi sincronização do teste, concorrência, animação ou defeito do carrossel. O sucesso em execução sequencial não prova que concorrência tenha sido a causa; a inspeção das transições também não prova uma corrida. Esta amostra finita não comprova ausência de intermitência.

**Não foi aplicada nem alegada uma correção funcional de causa.** A melhoria efetiva é a preservação de diagnóstico para que uma próxima ocorrência identifique a asserção, quantidade, subpasso e estado visual. Recomenda-se preservar esse log completo e a captura se houver recorrência; só então corrigir a causa demonstrada, sem adicionar retentativas ou relaxar assertions.

Apenas `scripts/test-avaliacoes.cjs` e este relatório foram alterados. Os arquivos de aplicação, inclusive o ícone PRONTO aprovado, scripts.js, CSS, cards, animações, indicadores e setas, permanecem iguais ao commit base. Pagamentos, status, histórico e H2 de desenvolvimento não foram alterados ou apagados. Os artefatos em /tmp e o helper forense são evidências locais temporárias; os resultados por execução estão preservados neste relatório versionado.

A publicação desta investigação utiliza somente `etapa-6-acompanhamento-pedidos`, sem merge ou PR. O SHA do commit é informado na entrega e verificável por `git rev-parse HEAD` e `git ls-remote origin refs/heads/etapa-6-acompanhamento-pedidos` (o documento não inclui o hash do próprio commit).
