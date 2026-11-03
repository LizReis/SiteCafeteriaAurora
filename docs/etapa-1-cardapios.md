# Etapa 1 — Cardápios persistidos

## Arquitetura e decisões

O fluxo das três páginas é `PaginaController → CardapioService → CategoriaRepository/ProdutoRepository → CardapioSecao/CardapioProduto → Thymeleaf`. `CatalogoApiController` também usa esse serviço; não consulta mais repositórios diretamente. A conversão ocorre dentro da transação somente de leitura, resolvendo a falha de sessão Hibernate do catálogo público.

MVC mantém o controller responsável pela requisição e pelo Model. SRP separa consultas públicas em `CardapioService` do CRUD em `CategoriaService`/`ProdutoService`. Repository Pattern utiliza Spring Data JPA. DTOs/records impedem dependência dos templates em entidades e proxies. A injeção por construtor explicita dependências. DRY concentra consulta, agrupamento e conversão para os três tipos no mesmo método. Não foram adicionadas interfaces ou hierarquias artificiais.

A consulta de uma página executa duas consultas: categorias ativas ordenadas por `ordemExibicao, id`, e produtos ativos das categorias ativas do tipo solicitado, com categoria carregada por `EntityGraph`. O agrupamento é feito em memória por ID. Não há consulta por produto ou por seção. Produtos têm ordenação estável por nome e ID. Categorias vazias permanecem visíveis com mensagem; menus sem categorias também têm mensagem.

## Arquivos

| Arquivo (sob `src/main`, salvo indicação) | Responsabilidade |
|---|---|
| `java/.../enums/TipoCardapio.java` (novo) | Classificação estável CAFE, DRINKS, COMIDAS |
| `java/.../model/Categoria.java` | Tipo e ordem, independentes do nome editável |
| `java/.../dto/CategoriaRequest.java`, `CategoriaResponse.java` | Contrato administrativo e validação dos novos campos |
| `java/.../service/CategoriaService.java` | Persiste tipo e ordem no cadastro/edição |
| `java/.../repository/CategoriaRepository.java` | Categorias ativas por tipo e ordem |
| `java/.../repository/ProdutoRepository.java` | Consulta pública por tipo e fetch de categoria |
| `java/.../dto/CardapioSecao.java`, `CardapioProduto.java` (novos) | Seções/produtos públicos, preço em centavos e BRL, fallback de imagem |
| `java/.../service/CardapioService.java` (novo) | Consultas e organização pública reutilizável |
| `java/.../controller/PaginaController.java` | Prepara `secoes` nas três páginas, preservando aliases `.html` |
| `java/.../controller/CatalogoApiController.java` | Delega consultas públicas ao serviço |
| `java/.../service/CompatibilidadeCategorias.java`, `config/CompatibilidadeConfig.java` (novos) | Compatibilidade dos registros antigos na inicialização |
| `java/.../service/CargaCardapioDev.java` (novo), `config/DadosExemploConfig.java` | Carga transacional, idempotente, exclusiva de dev |
| `resources/templates/cardapio-{cafe,drinks,comidas}.html` | Iterações reais mantendo estrutura e classes de cada design |
| `resources/static/js/cardapio-{cafe,drinks,comidas}.js` | Somente animações; remove contadores internos concorrentes |
| `resources/static/js/cart-store.js` | IDs reais, metadados do cadastro, estado e contadores centralizados |
| `resources/static/css/style.css` | Regras adicionais de vazio/indisponibilidade/disabled e correção de overflow dos steppers móveis |
| `resources/static/img/produto-sem-imagem.svg` (novo) | Placeholder neutro, sem atribuir foto de outro produto |
| `src/test/java/.../CardapioIntegracaoTests.java` (novo) | H2, MVC/Thymeleaf, CRUD, filtros, migração e Security |
| `src/test/java/.../CargaDevTests.java` (novo) | Carga coerente, imagens existentes e não sobrescrita |
| `src/test/java/.../service/CardapioServiceTest.java` (novo) | Consulta por tipo sem consulta por seção |
| `src/test/java/.../dto/CardapioProdutoTest.java` (novo) | Valores exatos, BRL e imagem alternativa |
| `README.md` | Exemplo administrativo atualizado e acesso à documentação |
| `scripts/test-cardapios.cjs` (novo) | Cenários funcionais A–E com Chromium, HTTP e reinícios reais |

`Produto`, seus DTOs/serviço administrativo, controllers administrativos e `SecurityConfig` foram reaproveitados. Os três CSS específicos, cabeçalho/rodapé, SVGs decorativos, gradientes, grades responsivas, fontes e animações foram preservados. O script de café agora também encadeia a revelação de categorias adicionais. Na inspeção móvel, a largura percentual antiga somada às margens cortava os botões à direita; um override restrito aos breakpoints existentes mantém as margens e deixa a largura automática para preservar o acesso aos controles.

## H2 e compatibilidade

Não há exclusão nem recriação automática do banco. Mantém-se `ddl-auto=update`: Hibernate adiciona `tipo_cardapio` e `ordem_exibicao` como campos anuláveis, permitindo abrir tabelas já preenchidas. Depois, o runner de compatibilidade executa antes da carga dev, preenchendo somente os campos novos ausentes.

O reconhecimento inicial contempla as categorias antigas `Cafés Especiais`, `Drinks e Bebidas`, `Doces e Salgados` e os seis nomes iniciais novos. Uma vez gravado o enum, renomear a categoria não altera seu pertencimento. Ordem antiga ausente recebe zero, com desempate por ID. Nomes personalizados desconhecidos ficam sem tipo: continuam cadastrados e disponíveis para consulta/edição administrativa, mas precisam ser classificados explicitamente para aparecer em uma das três páginas. Não se tenta adivinhar a classificação desses registros.

Não são alterados nome, descrição, atividade, disponibilidade, preço, imagem, ID nem associação dos produtos existentes. As categorias antigas amplas são preservadas como seções, em vez de repartir produtos arbitrariamente. O administrador pode criar seções mais específicas e mover produtos pelo CRUD. Fotos nulas antigas usam o placeholder; não são substituídas automaticamente por fotos presumidas.

A carga dev é transacional e somente ocorre quando **ambas** as tabelas estão vazias. Cria seis categorias (Espressos/Capuccino, Alcoólicos/Sucos, Salgados/Doces) e nove produtos coerentes com fotos existentes. As fotos foram inspecionadas visualmente (os textos alternativos anteriores não correspondiam sempre aos arquivos). Latte usa `42953.jpg`, cappuccino `1050.jpg`, frutas vermelhas `5493.jpg`, coquetel de café `9492.jpg`, suco de laranja `20691.jpg`, sanduíche `4368.jpg`, croissant `1320.jpg` e torta de frutas `2151900810.jpg`. Espresso recebe o placeholder neutro por não haver uma foto específica confirmada, evitando usar foto de latte. Se houver qualquer cadastro, não acrescenta, sobrescreve, reativa nem recria produtos removidos logicamente. Não se deve apagar `data/` para utilizar a implementação. Antes de aplicar atualizações em dados importantes, mantenha backup com a aplicação parada.

## Contrato administrativo

POST/PUT de categoria exigem `nome`, `ativa`, `tipoCardapio` e `ordemExibicao`; descrição é opcional. Tipo deve ser um dos três enums e ordem deve ser um inteiro não negativo. Exemplo:

```json
{"nome":"Salgados Artesanais","descricao":"Produção diária","ativa":true,"tipoCardapio":"COMIDAS","ordemExibicao":0}
```

PUT representa atualização completa: inclua esses campos também ao renomear. GET administrativo retorna a classificação, inclusive `null` para legados ainda não classificados. O CRUD de produtos mantém o contrato anterior, incluindo `categoriaId`, `imagemUrl`, `ativo` e `disponivel`.

A API pública de produtos preserva os campos anteriores e acrescenta propriedades de apresentação (`imagem`, `precoCentavos`, `precoFormatado`). O preço oficial continua `BigDecimal`; centavos são derivados exatamente no servidor. Não há aritmética monetária Java com `double`/`float`.

## Renderização e sacola

Cada página recebe `secoes` do mesmo serviço com seu enum. O nome da categoria é o título da seção. Cards expõem ID numérico real, preço inteiro em centavos e disponibilidade em atributos `data-*`. Nome, descrição, preço e imagem são renderizados pelo Thymeleaf com escape. A imagem aceita caminho local absoluto ou HTTPS; nulo/esquema inválido recebe placeholder, e falha de carregamento também tem fallback.

Produtos inativos e produtos de categorias inativas são excluídos na consulta do servidor. Indisponíveis permanecem no HTML, com indicação e ambos os botões desabilitados já na renderização. Não se depende de esconder cards no JavaScript.

A store é a única proprietária das quantidades. Cliques consultam a quantidade atual armazenada, atualizam o estado e renderizam todos os contadores. Retorno/reload restaura quantidades; limites são 0–99. A mesma chave de produto é usada em qualquer página. Preços não são mais extraídos do texto HTML: usa-se o inteiro calculado a partir do banco. Dados salvos são reconciliados com o catálogo público, atualizando nome, descrição, preço e imagem e removendo produtos indisponíveis/inativos.

O esquema local passa a `cafe-aurora.cart.v2`. A chave v1 é descartada: IDs demonstrativos não têm correspondência confiável com IDs do banco e não devem ser convertidos por nome. São rejeitados IDs inválidos e deduplicados os registros por ID. O carrinho continua temporário no navegador, sem endpoints de pedidos, pagamentos ou avaliações.

## Validação

Com JDK 21 e Maven Wrapper, executar `./mvnw clean test`. Neste ambiente, usar também a configuração de proxy/cache Maven preparada no onboarding:

```bash
export JAVA_HOME=/workspace/.aurora-setup/jdk-21
export MAVEN_USER_HOME=/workspace/.m2
export MAVEN_OPTS=-Djavax.net.ssl.trustStore=/etc/ssl/certs/java/cacerts
./mvnw -s /workspace/.aurora-setup/settings.xml -Dmaven.repo.local=/workspace/.m2/repository clean test
```

Para reproduzir os testes funcionais, gerar o JAR com `./mvnw package` (sem pular testes) e executar `node scripts/test-cardapios.cjs`. É necessário que `playwright` esteja disponível no Node e que Chromium exista; `CHROMIUM_PATH` pode selecionar o executável. A senha é gerada por execução sem impressão. Usa bancos e logs em uma pasta temporária. A variável opcional `LEGACY_H2_FILE` deve apontar para um arquivo H2 **fechado**: o teste migra uma cópia, preservando o original. O fixture legado usado nesta validação contém três categorias e nove produtos do onboarding.

## Preparação para Etapa 2

IDs e quantidades estão organizados para pedidos futuros, mas a sacola e seus preços locais não são confiáveis para cobrança. Na Etapa 2, o servidor deverá recarregar os IDs, validar quantidades/atividade/disponibilidade, recalcular totais e persistir o pedido em transação. Nenhuma dessas funcionalidades foi antecipada nesta etapa.

## Resultados e critérios de aceitação

A suíte completa executou **21 testes Java: 21 aprovados, zero falhas, erros ou ignorados**, incluindo os 13 testes anteriores. `clean test` e a geração do pacote foram executados com sucesso usando as opções de proxy/cache acima, sem `skipTests`.

O teste funcional usa Chromium real, HTTP Basic para o CRUD e perfil `dev` com H2 persistente em arquivo temporário. Valida A–D para cada tipo de cardápio e E por reinícios reais; também valida IDs distintos entre categorias, restauração da sacola, descarte de IDs antigos e retorno do histórico após editar a quantidade na sacola. Capturas desktop/móvel são geradas na pasta temporária informada ao final do script. A compatibilidade foi exercitada em cópia do H2 anterior (três categorias/nove produtos), iniciada duas vezes e comparada pelo conteúdo da API administrativa.

| Critério | Evidência |
|---|---|
| Três páginas leem H2; sem produtos fixos | MVC/Thymeleaf e cenários HTTP/navegador |
| Categorias estáveis, ordenadas e extensíveis | Serviço, testes de ordem, renomeação e movimentação |
| Cadastro/edição/preço/descrição/imagem refletidos | CRUD real e nova requisição/renderização |
| Produtos/categorias inativos filtrados | Testes de integração e cenários C/D |
| Indisponíveis visíveis e controles bloqueados | HTML `disabled`, teste MVC e Chromium |
| Imagens corretas/fallback | Inspeção dos arquivos, teste de recurso e carregamento no navegador |
| Preços brasileiros e centavos exatos | Testes de DTO, HTML e store |
| IDs reais e quantidades consistentes | Navegador entre os três menus, reload e sacola |
| Design e animações | Classes/estrutura preservadas, CSS específicos sem alteração, capturas e ausência de erros JavaScript |
| Todos os testes e início dev | Maven, carga dev e servidores reais de teste |
| Sem perda/duplicação ou reativação | Carga idempotente, reinícios e cópia do banco legado |

Não foi feita comparação automatizada de pixels nem cronometragem quadro a quadro das animações. Categorias legadas personalizadas sem tipo exigem classificação administrativa; isso é uma decisão explícita de compatibilidade, não uma tentativa de inferir pertencimento pelo nome nas consultas. O arquivo H2 original não foi apagado nem usado pelos testes destrutivos; a migração física foi validada em cópia.

Evidências desta execução: cenários completos em `/tmp/aurora-functional-MFyhji` (capturas desktop dos três menus e móvel); validação responsiva final em `/tmp/aurora-functional-16DidJ`. `VISUAL_ONLY=1 node scripts/test-cardapios.cjs` confirmou por coordenadas que todos os botões ficam dentro do viewport de 390 px nos três menus. O JAR foi atualizado após o ajuste CSS com `resources:resources jar:jar spring-boot:repackage` (sucesso). Logs locais: `/tmp/etapa1-final-tests.log`, `/tmp/etapa1-functional-final.log` e `/tmp/etapa1-visual.log`.
