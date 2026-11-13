# Etapa 5 — avaliações reais

## Resultado, escopo e Git

**APROVADA PARA REVISÃO**, em 13/11/2026. Implementado o fluxo real: pedido concluído → avaliação pendente com foto opcional → moderação → publicação das cinco avaliações aprovadas mais recentes na Home. Nenhum cadastro de cliente, pagamento, polling ou status novo foi implementado.

- Branch: `etapa-5-avaliacoes-reais`.
- Commit inicial: `80fbddff2fe43a7f49262134a578d39b20871b2d`.
- Antes das alterações: árvore limpa; branch local da Etapa 4. `git ls-remote origin refs/heads/main` e `git fetch origin main` confirmaram a main no SHA acima. A branch nova foi criada a partir de `FETCH_HEAD`.
- Commit da implementação validada: `477cb0dff5b0441550bccf138d6b6bd582f8e86c`.
- Este relatório integra um commit documental posterior. O SHA final, que inclui o próprio documento, é informado na entrega e pode ser obtido por `git rev-parse HEAD`, evitando um hash autorreferente impossível.
- Publicação: somente a branch de trabalho; o resultado e a igualdade do SHA remoto/local são conferidos ao fim da entrega. Nenhum merge ou PR foi solicitado ou criado.
- Total de arquivos da etapa: 37, incluindo este documento.

## Inventário e preservação

Foram inspecionados Pedido, ItemPedido, PedidoRepository, PedidoService, PedidoController, PedidoResponse, PaginaController, AdminPainelController, SecurityConfig, ApiExceptionHandler, configurações, templates e scripts de confirmação/avaliação/Home, CSS de confirmação/índice/responsividade, testes e relatório da Etapa 4.

A suíte inicial foi executada **antes da implementação**, em H2 isolado: **111 testes, todos aprovados, zero falhas/erros/ignorados**. As classes e cenários existentes foram mantidos. PedidoService, criação/cotação, cálculos, taxa, cadeia de status e StatusPagamento permaneceram inalterados. Os únicos acréscimos ao pedido são uma associação inversa de leitura, uma consulta específica para autorização e o campo booleano adicional da resposta.

O banco persistente `data/cafeaurora.mv.db` não foi aberto pelos novos testes nem submetido ao update de esquema desta etapa. Seu SHA-256, registrado antes da implementação e conferido após as validações, permaneceu:

```text
4fd9a52ebd8f77f8ac442dfc66e2b126c56f079f18b8e56c5e813ebcee0053b8
```

`sha256sum -c /workspace/.aurora/etapa5/h2-before.sha256` retornou `OK`. Essa evidência vale para o arquivo desta instância. Não houve exclusão, limpeza ou recriação de dados da usuária.

## Arquitetura, entidades e relacionamentos

| Componente | Responsabilidade |
|---|---|
| Avaliacao | Pedido obrigatório, nome de exibição, nota, comentário, consentimento, criação e moderação |
| AvaliacaoFoto | Associação única à avaliação, MIME validado e conteúdo binário normalizado em BLOB |
| StatusModeracao | `PENDENTE`, `APROVADA`, `REJEITADA` |
| AvaliacaoRepository / AvaliacaoFotoRepository | Persistência, existência, filtros, paginação e acesso separado ao binário |
| AvaliacaoRequest | Entrada validada e normalização de espaços |
| AvaliacaoPublica | Apenas conteúdo publicável e imagem; sem dados privados do pedido |
| AvaliacaoAdmin | Dados de revisão, vínculo interno ao pedido e indicação de foto, sem carregar binário na listagem |
| FotoAvaliacaoService | Limites de bytes/pixels, identificação, decodificação e reprocessamento |
| AvaliacaoService | Autorização, duplicidade, transação, publicação, moderação e fotos |
| AvaliacaoController | Recebimento multipart e respostas de fotos |
| AdminAvaliacaoController | Listagem e operações HTML de aprovação/rejeição |

O relacionamento pedido–avaliação é um para um. `avaliacoes.pedido_id` possui restrição **UNIQUE no banco**, além da validação no serviço. A foto possui sua própria chave única em `avaliacao_id`. Não há cascade de avaliação/foto para apagar pedidos. Rejeição preserva avaliação, vínculo e foto; não libera outro envio.

Campos obrigatórios: autor de 2–100 caracteres após strip/normalização de espaços, nota inteira de 1–5, comentário de 5–1000 após strip e consentimento explícito verdadeiro. Criação e moderação são definidas pelo servidor. O cliente não controla IDs, datas, status ou dados internos da foto.

O esquema é aditivo: tabelas `avaliacoes` e `avaliacoes_fotos`, sem remover ou modificar colunas históricas do pedido. O teste de compatibilidade da Etapa 4 foi reexecutado com as entidades novas: cria esquema anterior em `@TempDir`, inicia e atualiza duas vezes, preservando catálogo e pedido/item históricos. A migração não foi aplicada ao arquivo real durante os testes.

## Autorização, duplicidade e transação

1. A referência deve seguir o padrão existente `[A-Za-z0-9_-]{43}`.
2. O pedido correspondente é consultado com lock pessimista dentro da transação de avaliação.
3. Somente `CONCLUIDO` autoriza envio, para entrega ou retirada. Pagamento `PENDENTE` não bloqueia essa funcionalidade, conforme o escopo aprovado.
4. A existência de avaliação é verificada no serviço e protegida novamente por UNIQUE no banco.
5. Avaliação e fotografia são persistidas na mesma transação. Se a foto falhar, ambas são revertidas.
6. A criação sempre começa como `PENDENTE`; os campos do pedido não são alterados.

Duas requisições concorrentes válidas para o mesmo pedido produzem um sucesso e um conflito, sem foto órfã. Uma falha de integridade é tratada após o rollback; se já houver avaliação, retorna conflito. Outra falha de persistência exercitada retorna mensagem controlada, sem SQL, referência ou dados privados na resposta.

`PedidoResponse.jaAvaliado` é calculado pela existência da associação no banco, inclusive para avaliações pendentes ou rejeitadas. Não é uma flag em localStorage. Atualizar a página ou reiniciar a aplicação mantém a proibição de reenvio. Nenhum conteúdo de avaliação é incluído na resposta do pedido.

## Fotografia e segurança do upload

Foi escolhida tabela separada com BLOB no H2, para manter a consistência transacional sem arquivos órfãos, caminhos configuráveis ou rollback compensatório. Nenhuma foto é gravada em `static/img`; o nome original não é usado como caminho ou URL.

- Uma foto opcional; arquivo vazio ou múltiplos arquivos são rejeitados.
- Limite individual: **5 MiB** (`5 × 1024 × 1024` bytes; mensagem de interface “5 MB”). Requisição multipart: **6 MiB**.
- JPEG, PNG e WebP são reconhecidos pelo conteúdo e precisam corresponder ao MIME declarado. Extensão não é fonte de confiança.
- ImageIO precisa **decodificar** a imagem; assinatura isolada não basta. SVG, executáveis disfarçados, corrupção e incompatibilidade de MIME são rejeitados.
- Máximo de **4096 pixels por lado** e **4.000.000 de pixels totais**, conferido antes da decodificação dos pixels. A leitura do arquivo também é limitada a 5 MiB + um byte sentinela.
- Foi adicionada `com.twelvemonkeys.imageio:imageio-webp:3.15.3`, plugin ImageIO Java para decodificação WebP. O teste usa WebP real de 2×2 pixels, não somente cabeçalho. Não depende de executável nativo ou codec externo.
- A imagem decodificada é copiada para um raster novo e salva como **JPEG sem os metadados da entrada**, descartando EXIF, nome original e conteúdo anexado. Transparência é achatada em fundo branco. Saída também limitada a 5 MiB.
- Dimensões e bytes têm limites independentes; JPEG/PNG/WebP muito grandes precisam ser reduzidos pelo usuário antes do envio.

As imagens públicas exigem aprovação no momento da consulta. Fotos pendentes/rejeitadas ou inexistentes recebem 404. O administrador usa uma rota separada, protegida pela cadeia administrativa. Todas as respostas de imagem usam `image/jpeg`, `X-Content-Type-Options: nosniff` e `Cache-Control: no-store`; a política evita reutilizar imagem em cache após rejeição posterior.

Os testes versionam somente código e pequenas constantes sintéticas de pixels para decodificação; não foram versionados arquivos binários, fotografias de pessoas, bancos, logs ou capturas.

## Contratos HTTP

| Rota | Contrato |
|---|---|
| `POST /api/avaliacoes` | Multipart público, com CSRF; campos `referencia`, `nomeAutor`, `nota`, `comentario`, `consentimentoPublicacao=true`, foto opcional `foto` |
| `GET /api/avaliacoes/{id}/foto` | JPEG somente de avaliação aprovada, ou 404 |
| `GET /admin/avaliacoes` | Login ADMIN; filtro opcional `status` e `pagina`, tamanho 15; página negativa normalizada para zero |
| `GET /admin/avaliacoes/{id}/foto` | Foto para revisão, inclusive pendente/rejeitada; exige ADMIN |
| `POST /admin/avaliacoes/{id}/aprovar` | Exige ADMIN e CSRF; atualiza status/data e redireciona à listagem |
| `POST /admin/avaliacoes/{id}/rejeitar` | Exige ADMIN e CSRF; preserva conteúdo e remove de consultas públicas subsequentes |
| `GET /api/pedidos/consulta/{referencia}` | Contrato anterior preservado com `jaAvaliado` adicional |
| `GET /` | Thymeleaf consulta até cinco avaliações aprovadas, por `criadaEm DESC, id DESC` |

O POST público rejeita query string, campos extras/repetidos, IDs, status, datas, nota fracionária e partes de arquivo com nome não permitido. A referência vem do fragmento `#pedido=...` e é enviada **somente no corpo multipart**. O frontend não coloca esse token na query ou no nome do arquivo.

Respostas públicas:

- **201:** somente `{"mensagem":"Avaliação registrada e aguardando aprovação"}`, com `no-store`; não devolve referência nem dados do pedido.
- **400:** campos/consentimento/foto inválidos, repetidos ou não permitidos.
- **403:** CSRF ausente/inválido; um visitante não adquire permissão administrativa com referência de pedido.
- **404:** referência inexistente ou malformada; foto inexistente ou não pública.
- **409:** pedido não concluído ou já avaliado.
- **413:** limite de arquivo/requisição excedido, com mensagem controlada.
- **415:** tipo de requisição incompatível com multipart.
- **500 controlado:** falha de persistência exercitada pelo teste de rollback.

Um visitante em navegação HTML administrativa é redirecionado ao login, conforme a cadeia existente. Nenhuma regra de SecurityConfig foi alterada: HTTP Basic de `/api/admin/**` continua separado da sessão HTML; CSRF permanece ativo nos novos POSTs públicos e administrativos.

## Moderação

As avaliações começam `PENDENTE`. As operações administrativas explícitas podem passar de qualquer estado para `APROVADA` ou `REJEITADA`, inclusive rever uma decisão anterior. Repetir uma decisão mantém o estado e registra a nova data de revisão. Não existe operação pública de moderação nem rota para voltar a `PENDENTE`.

Aprovar uma rejeitada exige clique explícito em “Aprovar”. Rejeitar uma aprovada a remove da Home e bloqueia sua foto nas próximas consultas. Autor, nota, comentário, consentimento e criação não são editados pela moderação. O pedido e o pagamento não são modificados.

A página reutiliza cabeçalho, tabela, filtro, paginação, botões e estilos administrativos. Todos os menus administrativos existentes mantêm seus links e recebem “Avaliações”.

## Integração com Pedido Confirmado

O protótipo foi substituído por `fetch` real com FormData e cabeçalho CSRF do fragmento existente. O navegador define o boundary; não há Content-Type multipart manual.

O botão só é habilitado para `CONCLUIDO` e `jaAvaliado=false`. O backend repete as mesmas verificações. Durante o envio, o botão e cancelamento são desabilitados e há guarda contra cliques repetidos. Foi acrescentado consentimento explícito, usando a paleta e tipografia existentes.

Somente 201 ou consulta posterior que confirme `jaAvaliado=true` dispara a tela de agradecimento. Ela informa **registro aguardando aprovação**, sem prometer publicação. Foto temporária e Object URL são liberadas no sucesso; a URL também é liberada ao sair da página.

Em erro, o painel continua aberto, comentário e foto permanecem disponíveis e o envio pode ser tentado novamente quando permitido. Em falha de rede ou resposta perdida após commit, uma nova consulta do pedido confirma se o registro ocorreu. O teste de navegador interrompe deliberadamente a resposta depois do 201 e confirma a recuperação correta.

Foram preservados painéis, estrelas, prévia/remoção da foto, cancelamento, animações, aria-hidden, inert, foco, suporte a movimento reduzido e versão mobile. Nenhum conteúdo do cliente é inserido com innerHTML.

## Home e carrossel

Os cinco exemplos fictícios foram removidos. A renderização usa `th:text`, inclusive para strings maliciosas, e DTO sem nome do comprador, telefone, email, endereço, observações, ID do pedido ou referência de acompanhamento.

A imagem usa foto aprovada quando existir. Sem foto, tenta o caminho histórico seguro do item do pedido, com fallback genérico já existente; não inventa uma foto enviada pelo cliente. O avatar usa logo local em lugar de enviar nomes a um serviço externo.

O carrossel usa JavaScript nativo para não depender do jQuery externo no carregamento. As mesmas classes center/near/far, transitions, animação de entrada e IntersectionObserver foram preservados. Não se modificaram cores, fontes, gradientes, menus públicos ou animações CSS. Ajustes CSS limitam-se a quebrar textos longos e impedir que o checkbox de consentimento herde a largura dos campos de texto.

| Quantidade aprovada | Comportamento |
|---|---|
| 0 | Estado vazio, sem cards/indicadores/setas |
| 1 | Um card central, sem controles inúteis |
| 2–4 | Cards reais, um destaque, setas e indicadores correspondentes |
| 5 | Cinco cards, classes e efeito visual existentes |
| Mais de 5 | Somente os cinco mais recentes na Home; todas preservadas no banco |

Setas rotacionam elementos reais, sem clones. Indicadores apontam para cada card; há somente um listener por controle. Comentário fica visível apenas no destaque, conforme o CSS existente.

## Testes e resultados

Todos os testes novos usam `jdbc:h2:mem:avaliacoes_test;DB_CLOSE_DELAY=-1`, perfil test e credenciais fictícias próprias. A limpeza inclui foto → avaliação → pedido/itens → cotação → produto → categoria, apenas nesse contexto. As bases isoladas da Etapa 4 foram mantidas.

| Grupo final | Executados | Aprovados | Falhas/erros | Ignorados |
|---|---:|---:|---:|---:|
| AvaliacaoIntegracaoTests | 42 | 42 | 0 | 0 |
| AdminIntegracaoTests | 48 | 48 | 0 | 0 |
| PedidoIntegracaoTests | 34 | 34 | 0 | 0 |
| CardapioIntegracaoTests | 4 | 4 | 0 | 0 |
| PaginasCompartilhadasTests | 8 | 8 | 0 | 0 |
| CompatibilidadeLegadoTests | 1 | 1 | 0 | 0 |
| CargaDevTests + CafeAuroraApplicationTests | 2 | 2 | 0 | 0 |
| Unitários de DTO/modelos/serviços | 14 | 14 | 0 | 0 |
| **Total** | **153** | **153** | **0** | **0** |

`verify` passou e a suíte foi repetida em ordem inversa com os mesmos 153 aprovados. Os XMLs de Surefire foram somados e conferidos, não apenas o status do Maven. Não foi instalado plugin para produzir um percentual de cobertura.

### Matriz de cobertura

| Área | Casos exercitados |
|---|---|
| Autorização | Formato/referência inexistente 404; recebido, preparo, pronto e cancelado 409; entrega e retirada concluídas; pagamento pendente preservado |
| Entrada | Nome curto/vazio/longo; nota 0/6/null/fracionária; comentário vazio/curto/longo; consentimento false/ausente; query e campos internos rejeitados |
| Duplicidade | Serviço, constraint diretamente no banco, duas requisições concorrentes com foto, rejeição não libera reenvio, jaAvaliado e reinício real |
| Upload | Sem foto; JPEG/PNG/WebP reais; vazio, corrupção, disfarce SVG, MIME divergente, >5 MiB, múltiplos arquivos, largura excessiva e limite de pixels independente |
| Transação | Constraint deliberada falhando na tabela de foto; avaliação e binário revertidos; mensagem sem referência e nova tentativa válida |
| Fotos | Pendente 404 público; ADMIN visualiza; aprovada 200 com JPEG/nosniff/no-store; rejeitada 404; binário confirmado após reinício |
| Segurança | POST público sem login e com CSRF; token ausente/inválido 403; HTML administrativo protegido; aprovação/rejeição autenticadas; sem CSRF bloqueado; inexistente 404 |
| Moderação | Pendência inicial, data de revisão, aprovação/rejeição, original preservado, filtros, páginas e desempate por ID |
| Publicação | Zero a sete armazenadas, máximo cinco aprovadas, ordenação, ausência de pendentes/rejeitadas, texto escapado e fallback histórico |
| Navegador | Envio/foto, erro mantendo formulário, resposta perdida após commit, obrigado, reload sem reenvio, moderação, mobile, setas/indicadores, limite HTTP e persistência após reinício |
| Regressão | Todos os 111 anteriores e scripts existentes de catálogo/checkout, mantendo idempotência, histórico, segurança e status |

### Ponta a ponta

`scripts/test-avaliacoes.cjs` utiliza Java 21, Playwright e Chromium com banco novo em `/tmp`, senha aleatória e porta local 18083. Cria pedidos pela API real e avança os status pelo painel HTML. Envia sete avaliações de nomes fictícios, com um cenário de foto, aprova, verifica o limite de cinco, rejeita e confirma retirada pública.

Exercita 0/1/2/3/4/5 cards, setas, indicadores, ausência de duplicação, largura mobile 390 px e administração mobile. Confirma HTTP 413 e mensagem de limite usando o parser real do Tomcat, que MockMvc não exercita. Após reiniciar, confirma jaAvaliado, recusa duplicidade 409, lê os bytes da foto pública e verifica 404 após rejeitá-la.

Os scripts existentes `test-pedidos.cjs` e `test-cardapios.cjs` também concluíram com código 0, em bancos temporários próprios e portas distintas. Cenários dos scripts não foram somados ao total JUnit porque os runners não apresentam contagem individual compatível.

### Falhas intermediárias e correções

- Na primeira suíte nova, cinco expectativas de Home falharam porque o helper de teste devolvia sempre a primeira avaliação ao criar múltiplas fixtures. Foi corrigido para localizar a avaliação pelo pedido da iteração; o código de ordenação não foi afrouxado.
- O primeiro teste de navegador alterava apenas o fragmento da mesma página já carregada sem pedido; isso não reexecuta o script inicial. O teste passou a recarregar explicitamente, conforme o cenário de reabertura exigido.
- Uma tentativa foi invalidada por reconstrução do JAR enquanto o servidor de teste o utilizava, provocando ClassNotFound no processo. O runner novo agora copia o JAR para o próprio diretório temporário antes de iniciar. As execuções posteriores completas passaram, sem desativar verificações.
- Os logs de falha foram mantidos fora do Git; o runner imprime somente a primeira linha de erro para não divulgar cabeçalhos/cookies/CSRF de relatórios detalhados do Playwright.

## Comandos e evidências

Executados na raiz `/workspace/SiteCafeteriaAurora`:

```bash
git status --short
git branch --show-current
git ls-remote origin refs/heads/main
git fetch origin main
git switch -c etapa-5-avaliacoes-reais FETCH_HEAD
bash /workspace/.aurora/maven.sh test
bash /workspace/.aurora/maven.sh -DskipTests package
bash /workspace/.aurora/maven.sh -Dtest=AvaliacaoIntegracaoTests test
bash /workspace/.aurora/maven.sh verify
bash /workspace/.aurora/maven.sh -Dsurefire.runOrder=reversealphabetical test
JAVA_HOME=/workspace/.aurora/java-21 MAVEN_USER_HOME=/workspace/.m2 node scripts/test-avaliacoes.cjs
JAVA_HOME=/workspace/.aurora/java-21 MAVEN_USER_HOME=/workspace/.m2 node scripts/test-pedidos.cjs
JAVA_HOME=/workspace/.aurora/java-21 MAVEN_USER_HOME=/workspace/.m2 node scripts/test-cardapios.cjs
node --check scripts/test-avaliacoes.cjs
node --check src/main/resources/static/js/scripts.js
node --check src/main/resources/static/js/pedido-avaliacao.js
sha256sum -c /workspace/.aurora/etapa5/h2-before.sha256
git -c core.whitespace=cr-at-eol diff --check
```

O helper externo usa JDK 21.0.13, Maven Wrapper 3.10.0, proxy e cache gravável do ambiente. Em uma instalação normal com JDK 21, o equivalente é `./mvnw verify`. O parâmetro de whitespace reconhece os CRLF já existentes em index.html; não ignora espaços finais de conteúdo. O build emitiu aviso de metadata de repositório snapshot inacessível, mas resolveu as dependências necessárias e concluiu sem desativar TLS ou checksums.

Evidências locais, não versionadas:

- `/workspace/.aurora/etapa5/baseline.log`: 111 aprovados antes da implementação.
- `avaliacoes.log`: execução inicial com falha do helper descrita acima.
- `full.log`: 149 aprovados na revisão intermediária.
- `final.log` e `final-verify.log`: 152 aprovados em revisões intermediárias.
- `verify-final.log`: **153 aprovados, BUILD SUCCESS**, na implementação final.
- `estabilidade.log`: **153 aprovados, BUILD SUCCESS**, ordem inversa.
- `aceitacao-final.log`: último E2E completo, código **0**.
- `regressao-pedidos.log`, `regressao-cardapios.log`: runners existentes, código **0**.
- `target/surefire-reports/`: XML e texto da execução mais recente.
- `/tmp/aurora-avaliacoes-bEvLE6/`: capturas de 0–5 cards mobile, formulário desktop/mobile e moderação mobile, aplicativo e banco fictícios usados no E2E final. As capturas de formulário e carrossel foram inspecionadas.
- `/tmp/aurora-pedidos-AtlosW/` e `/tmp/aurora-functional-cK6fI8/`: evidências das regressões existentes.

Esses caminhos pertencem à instância atual, podem expirar e não são arquivos entregues no Git. Código, testes e resumo verificável dos resultados estão versionados.

## Arquivos modificados e motivo

Prefixo Java abaixo: `src/main/java/br/com/cafeaurora/`.

| Arquivo | Motivo |
|---|---|
| `model/Avaliacao.java` | Entidade, unicidade por pedido, consentimento e moderação |
| `model/AvaliacaoFoto.java` | BLOB separado e vínculo único |
| `model/Pedido.java` | Associação inversa de leitura para jaAvaliado |
| `enums/StatusModeracao.java` | Estados de revisão |
| `repository/AvaliacaoRepository.java` | Existência, filtros e persistência |
| `repository/AvaliacaoFotoRepository.java` | Consultas de foto e presença sem carregar binário na lista |
| `repository/PedidoRepository.java` | Busca por referência com lock para avaliação |
| `dto/AvaliacaoRequest.java` | Entrada normalizada e validada |
| `dto/AvaliacaoPublica.java` | Publicação mínima sem dados privados do pedido |
| `dto/AvaliacaoAdmin.java` | Dados para revisão administrativa |
| `dto/PedidoResponse.java` | Campo booleano aditivo jaAvaliado |
| `service/AvaliacaoService.java` | Regras, transação, consulta e moderação |
| `service/FotoAvaliacaoService.java` | Validação e normalização real de imagem |
| `controller/AvaliacaoController.java` | Multipart e respostas de fotos |
| `controller/AdminAvaliacaoController.java` | Listagem, aprovação e rejeição |
| `controller/PaginaController.java` | DTOs aprovados no modelo da Home |
| `exception/AvaliacaoPersistenciaException.java` | Mensagem controlada para persistência |
| `exception/ApiExceptionHandler.java` | Respostas controladas de upload/persistência |

Demais arquivos:

| Arquivo | Motivo |
|---|---|
| `pom.xml` | Decoder WebP TwelveMonkeys 3.15.3 |
| `src/main/resources/application.properties` | Limites multipart 5/6 MiB |
| `src/main/resources/static/js/pedido-avaliacao.js` | Envio real, CSRF, erros e recuperação de confirmação |
| `src/main/resources/static/js/pedido-confirmado.js` | Consumo do jaAvaliado e consulta de recuperação |
| `src/main/resources/static/js/scripts.js` | Carrossel variável e mesmas animações sem dependência externa de jQuery |
| `src/main/resources/static/css/pedido-confirmado.css` | Checkbox com largura nativa |
| `src/main/resources/static/css/responsive.css` | Quebra de textos longos de avaliações |
| `src/main/resources/templates/pedido-confirmado.html` | Consentimento, textos reais e versões de scripts |
| `src/main/resources/templates/index.html` | Cards/indicadores reais, vazio e conteúdo escapado |
| `src/main/resources/templates/admin/avaliacoes.html` | Tela nova reutilizando estilos administrativos |
| `src/main/resources/templates/admin/index.html` | Link Avaliações |
| `src/main/resources/templates/admin/categorias.html` | Link Avaliações |
| `src/main/resources/templates/admin/produtos.html` | Link Avaliações |
| `src/main/resources/templates/admin/pedidos.html` | Link Avaliações |
| `src/main/resources/templates/admin/pedido.html` | Link Avaliações |
| `src/test/java/br/com/cafeaurora/AvaliacaoIntegracaoTests.java` | 42 cenários com filtros, serviços e banco reais |
| `scripts/test-avaliacoes.cjs` | E2E isolado, mobile, erro, fotos, moderação e reinício |
| `README.md` | Uso, limites, moderação e comandos de testes |
| `docs/etapa-5-avaliacoes-reais.md` | Este relatório |

## Limitações e próximas etapas

Não há falha crítica aberta nos cenários executados. A fixture de esquema anterior e o H2 local não equivalem a uma homologação em todo banco externo possível. Não houve teste de carga prolongado, auditoria independente de codecs ou teste em todos os navegadores; o navegador automatizado foi Chromium.

Para um ambiente de produção ainda seriam necessários política de retenção/remoção de avaliações e fotos, atendimento a solicitações dos titulares, backup/restauração, gestão de armazenamento e limites de requisição/concorrência. Consentimento está registrado, mas não substitui uma política de privacidade formal. Comentários e fotos precisam continuar sendo revisados pela equipe antes de aprovar. As bibliotecas de imagem devem ser mantidas atualizadas.

Essas são recomendações de implantação e operação, não bloqueios do fluxo de portfólio implementado. Pagamentos, acompanhamento automático, mudanças de entrega/retirada e eventual EM_ENTREGA continuam fora desta etapa. Não houve alteração no fluxo financeiro ou no menu público. Encerrar na Etapa 5 e aguardar revisão antes de merge ou Etapa 6.
