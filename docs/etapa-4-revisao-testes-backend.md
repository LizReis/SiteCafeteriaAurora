# Etapa 4 — auditoria e testes do backend

## Resultado e rastreabilidade

**APROVADA PARA REVISÃO**, em 13/11/2026. Escopo: estabilizar os fluxos existentes, preservar dados e ampliar testes. Não houve alteração de código de produção, templates, CSS, JavaScript, contratos, dependências ou lockfiles. Não se iniciou a Etapa 5.

- Branch: `etapa-4-revisao-testes-backend`.
- Commit inicial: `ad59d8df13dacd6a19ee80ef13badcf445afaa31`.
- Estado inicial: branch local `work`, árvore limpa. `git ls-remote origin refs/heads/main` confirmou o mesmo SHA da main remota; a branch de trabalho foi criada nesse ponto.
- Commit final de implementação/testes: `22e1534ef17d633e7367a9728be97673c14a1148`.
- Esse commit foi publicado na branch de trabalho. Este relatório é um commit documental posterior; seu SHA final é fornecido na entrega e pode ser obtido com `git rev-parse HEAD` (um documento não pode conter o hash do próprio commit).
- Nenhum merge na main, Pull Request ou alteração de banco foi realizado. O aviso automático do GitHub com URL para criar PR não significa criação de PR.
- Alterações: 8 arquivos, incluindo este relatório. Logs, caches e bancos não fazem parte dos commits.

## Proteção do H2 e isolamento

Antes da suíte desta etapa, foram revisadas todas as ocorrências de `@SpringBootTest`, as configurações de teste e as operações `deleteAll`. A execução de desenvolvimento iniciada no onboarding foi encerrada para evitar escrita concorrente e reinício por DevTools durante a auditoria.

O risco indicado em `CardapioIntegracaoTests` precisava ser qualificado: **já existia** `src/test/resources/application.properties` com URL em memória e credenciais fictícias. Não foi observado acesso desse teste ao arquivo persistente. A fragilidade concreta era a dependência de configuração global e o compartilhamento de `cafeaurora_test` entre contextos diferentes. Agora cada classe declara explicitamente sua URL; as classes normais também fixam o perfil `test`, impedindo carga dev acidental por seleção de perfil externo.

| Classe | Banco / perfil |
|---|---|
| CafeAuroraApplicationTests | `jdbc:h2:mem:contexto_test;DB_CLOSE_DELAY=-1`, test |
| PaginasCompartilhadasTests | `jdbc:h2:mem:paginas_test;DB_CLOSE_DELAY=-1`, test |
| CardapioIntegracaoTests | `jdbc:h2:mem:cardapio_test;DB_CLOSE_DELAY=-1`, test |
| PedidoIntegracaoTests | `jdbc:h2:mem:pedidos_tests;DB_CLOSE_DELAY=-1`, test |
| AdminIntegracaoTests | `jdbc:h2:mem:admin_etapa4;DB_CLOSE_DELAY=-1`, test |
| CargaDevTests | `jdbc:h2:mem:carga_dev;DB_CLOSE_DELAY=-1`, dev proposital |
| CompatibilidadeLegadoTests | arquivo criado por `@TempDir`, URL absoluta explícita; dois contextos dev, `ddl-auto=update` |

Os testes normais usam `create-drop` exclusivamente nesses bancos em memória. Credenciais são fictícias e definidas apenas nos testes. A suíte de pedidos também verifica a URL real da conexão e a ausência do bean de carga dev. As classes que removem registros possuem bancos distintos e limpeza em `@BeforeEach`; os testes unitários usam objetos/mocks sem acesso ao H2 de desenvolvimento.

Os scripts de navegador criam seus próprios diretórios em `/tmp`, com senha aleatória por processo e portas 18081/18082. **Não foi fornecida a variável `LEGACY_H2_FILE` apontando ao banco da usuária.** O cenário de esquema antigo foi exercitado pelo teste Java com fixture inteiramente fictícia.

Fingerprint de `data/cafeaurora.mv.db`, coletado após o encerramento do servidor e durante a auditoria, e confirmado nas verificações posteriores:

```text
4fd9a52ebd8f77f8ac442dfc66e2b126c56f079f18b8e56c5e813ebcee0053b8
```

`sha256sum -c /workspace/.aurora/etapa4/h2-before.sha256` retornou `OK`. Esse fingerprint é evidência desta instância, não de um banco externo ou do estado anterior ao onboarding. Nenhuma URL de teste aponta para `./data/cafeaurora`; não houve exclusão ou recriação desse banco.

## Inventário e avaliação da arquitetura

| Componentes inspecionados | Responsabilidade e conclusão |
|---|---|
| AdminPainelController | Binding/validação de formulários, modelos Thymeleaf, flashes e redirecionamentos; reutiliza serviços e mantém CSRF. Testes novos exercitam templates reais. |
| AdminCategoriaController, AdminProdutoController | CRUD REST, Bean Validation, respostas 201/200/204; regras de negócio delegadas aos serviços. |
| PedidoController | Cotação, criação 201 e consulta por referência, com `no-store`; preservado. |
| CatalogoApiController, PaginaController | API pública e páginas consultam dados atuais; taxa de checkout vem da configuração. |
| CategoriaService, ProdutoService | Transações, duplicidade ignorando caixa, validação da categoria e exclusão lógica; sem necessidade de reescrita. |
| CardapioService | Filtro de ativos, classificação por tipo e ordenação das seções; indisponível pode aparecer desabilitado, distinto de inativo. |
| PedidoService | Validação, preço oficial, lock de produtos, cotação com hash e 900 s de validade, idempotência e transação de pedido/itens; rollback e concorrência já testados. |
| AdminPedidoService | Indicadores reais, paginação de 15, ordenação por data/ID, detalhes e atualização validada pelo domínio. Página negativa normalizada para zero. |
| CompatibilidadeCategorias | Preenche classificação legada reconhecida sem reativar ou sobrescrever edições; desconhecidas permanecem para classificação administrativa. |
| CargaCardapioDev, DadosExemploConfig | Somente dev; inserção apenas com catálogo vazio. Reinício não duplica nem reativa dados. |
| CompatibilidadeConfig | Executa compatibilidade antes da carga dev. |
| Pedido, ItemPedido | Valores monetários `numeric(10,2)`, snapshots de nome/preço/imagem, cascade dos itens; produto e categoria não são excluídos pela desativação. |
| Categoria, Produto | Categoria obrigatória para produto, flags independentes, classificação legada nullable. |
| CotacaoPedido | Token armazenado como hash e expiração; consumo protegido pela unicidade da cotação no pedido. |
| Cinco repositories | Consultas filtradas, EntityGraph para leituras necessárias, restrições únicas e lock pessimista de produtos na compra. |
| DTOs de categoria/produto, cotação/pedido/endereço/itens e respostas | Bean Validation, limite monetário, quantidade por item e pedido; rejeição de campos não autorizados no checkout. Resposta pública omite nome, telefone, email e observações; endereço faz parte do contrato existente. |
| Enums StatusPedido, StatusPagamento, TipoAtendimento, FormaPagamento, TipoCardapio | Mantidos; `PRONTO` é preparado, pagamento inicial `PENDENTE`, sem `EM_ENTREGA`. |
| SecurityConfig | Duas cadeias: API administrativa Basic stateless exige ADMIN; HTML com sessão e CSRF. Console H2 negado. |
| ApiExceptionHandler e configurações | Validação 400, inexistência 404, conflito 409, persistência 500 com mensagem controlada; testes confirmam ausência de SQL/contato nos erros de persistência. |
| Templates e JavaScript do checkout/cardápios | Contratos conferidos por MockMvc e cenários reais de navegador; nenhum arquivo visual alterado. |

As regras principais permanecem em serviços/domínio. Não foram introduzidas abstrações nem mudanças de arquitetura por estilo. Os testes de integração usam filtros reais e serviços reais, sem mocks para encobrir persistência ou segurança.

## Situação anterior, achados e correções

1. **Isolamento implícito/compartilhado:** contexto e páginas não declaravam URL própria, e cardápio dependia do arquivo global enquanto executava limpeza. Causa: configuração comum com um único nome de banco. Correção: URLs explícitas distintas e perfil test nas classes normais. Verificação: linha de base de 55 testes, novas suítes e repetição com ordem invertida; asserção da URL real em `perfilNormalNaoCarregaDadosDevEBancoEhMemoria`.
2. **Lacuna de regressão administrativa:** havia 55 testes, mas não uma suíte que exercitasse login/logout, isolamento de cadeias, formulários reais e gerenciamento de pedidos. Correção: `AdminIntegracaoTests`, 48 casos, incluindo as 25 combinações de status. Não foi detectada necessidade de corrigir os controllers ou a segurança.
3. **Lacunas de pedidos e compatibilidade:** adicionados sete casos executados à suíte de pedidos (34 no total) e um teste de atualização de esquema antigo com dois inícios. São ampliações de cobertura, não alegações de bugs de produção.
4. **README contraditório:** uma seção ainda afirmava ausência do painel HTML. Corrigida para Etapa 3 implementada; atualizada também a descrição de CSRF do painel e adicionada orientação de teste isolado.

Durante a construção da suíte administrativa ocorreram quatro falhas de expectativa: requisições sem `Accept: text/html` esperavam redirecionamento, mas recebiam 401. Com negociação HTML explícita, restou uma expectativa incorreta de padrão de URL: a resposta real era `/admin/login`. Os testes foram corrigidos para os contratos observados. A execução seguinte passou 45 casos; as adições finais elevaram o grupo a 48. Nenhuma proteção foi relaxada para obter aprovação.

**Não se confirmou defeito de produção nos cenários auditados.** Isso não representa prova de ausência de todo defeito possível.

## Matriz de cobertura efetiva

| Fluxo / endpoint | Evidência automatizada |
|---|---|
| `/admin/login`, `/admin/logout`, `/admin` | Login válido, usuário/senha inválidos, logout invalida sessão, nova autenticação, CSRF no login/logout, usuário USER sem privilégios mesmo enviando role ADMIN. |
| `/api/admin/**` vs `/admin/**` | Sessão HTML não autoriza GET/POST da API; Basic válido sem sessão, inválido 401, USER 403; HTML protegido e negociação de conteúdo. |
| CSRF e `/h2-console/` | Formulários de catálogo/status/checkout sem token ou token inválido bloqueados; console bloqueado para visitante e administrador. Basic mantém exceção própria. |
| CRUD `/api/admin/categorias` | Criação, edição, consulta, nome próprio, duplicidade sem distinguir caixa, validação de tipo/ordem/nome/obrigatórios, inexistência e desativação sem remoção. |
| CRUD `/api/admin/produtos` | Criação e edição refletidas nos três menus, nome/descrição/preço/imagem, categorias inválidas/inativas, obrigatórios, preço zero/negativo/excesso de dígitos/precisão, desativação e indisponibilidade independentes. |
| `/admin/categorias`, `/admin/produtos` | Renderização, edição preenchida, criação/edição/desativação via POST real, erros de campo e negócio com dados do formulário preservados. |
| `/api/categorias`, `/api/produtos`, `/api/produtos/{id}` e três cardápios | Ordenação/classificação, seções vazias, ocultação de inativos e categoria desativada, indisponível desabilitado; fallback de imagem insegura/ausente. Imagem local inexistente não é rejeitada pelo DTO atual; não foi criada regra nova. |
| `POST /api/pedidos/cotacao` | Um/múltiplos itens, valores oficiais, entrega/retirada, limites de unidades e monetário, duplicados, lista vazia, produto inexistente/inativo/indisponível/categoria inativa, campos manipulados e cache. |
| `POST /api/pedidos` | Criação 201, endereço de entrega, contatos individualmente inválidos, validade/expiração/cotação inexistente, mudança de preço/disponibilidade, campos extras, rollback ao falhar item e sanitização da resposta. |
| Idempotência | Reenvio, resposta perdida, mesma chave com dados diferentes, ausência/formato inválido, outra chave com mesma cotação, duas criações concorrentes, recuperação em outra transação e contagens no banco. |
| `GET /api/pedidos/consulta/{referencia}` | Referência válida, inexistente e inválida, sem login, sem consulta por ID, no-store, snapshots históricos e status persistido. Visitante não atualiza status. |
| `/admin` e `/admin/pedidos` | Indicadores derivados do banco com zero/pedidos reais, recentes, filtros com cada status povoado, lista vazia, ordem, páginas 0/1/inexistente/negativa, filtro inválido 400. |
| `/admin/pedidos/{id}` e POST status | Detalhes reais e legados com null, 404, 25 pares de transições, flash de sucesso/erro, persistência e consulta pública, pagamento permanece PENDENTE, autenticação/CSRF. |
| Compatibilidade/carga dev | Esquema antigo criado do zero em arquivo temporário, atualização e duas inicializações sem perda; classificação reconhecida e personalizada, edições e flags preservadas; carga não duplica. |
| Frontend integrado | Playwright: sacola entre cardápios, preços atualizados, indisponibilidade, checkout entrega/retirada, confirmação/reload, falhas preservam sacola, idempotência e reinício. |

## Execução e resultados

Ambiente: Linux, JDK 21.0.13 completo em `/workspace/.aurora/java-21`, Maven Wrapper 3.10.0. O helper externo configura proxy Maven e cache gravável sem modificar o checkout ou desativar TLS. Nenhuma dependência foi adicionada ao projeto.

| Grupo final | Executados | Aprovados | Falhas/erros | Ignorados |
|---|---:|---:|---:|---:|
| AdminIntegracaoTests | 48 | 48 | 0 | 0 |
| PedidoIntegracaoTests | 34 | 34 | 0 | 0 |
| CardapioIntegracaoTests | 4 | 4 | 0 | 0 |
| PaginasCompartilhadasTests | 8 | 8 | 0 | 0 |
| CompatibilidadeLegadoTests | 1 | 1 | 0 | 0 |
| CargaDevTests + CafeAuroraApplicationTests | 2 | 2 | 0 | 0 |
| DTOs, modelos e serviços unitários | 14 | 14 | 0 | 0 |
| **Total Maven final** | **111** | **111** | **0** | **0** |

Execuções intermediárias: baseline 55/55; grupo ampliado 80/80; primeira suíte final 109/109; após dois cenários adicionais, verify 111/111 e repetição em ordem inversa 111/111. Cada total corresponde à respectiva revisão, não são somados como testes distintos.

Playwright: dois scripts concluídos com código 0. Não são somados ao total JUnit porque os scripts não fornecem contagem de testes individuais. `test-cardapios.cjs` registrou sacola, A–D nos três tipos e persistência E; `test-pedidos.cjs` registrou entrega, retirada, falhas, retry e verificação JDBC após reiniciar.

Comandos executados na raiz do repositório (logs completos fora do checkout):

```bash
git status --short
git branch --show-current
git rev-parse HEAD
git ls-remote origin refs/heads/main
git switch -c etapa-4-revisao-testes-backend
bash /workspace/.aurora/maven.sh test
bash /workspace/.aurora/maven.sh -Dtest=AdminIntegracaoTests test
bash /workspace/.aurora/maven.sh -DskipTests package
bash /workspace/.aurora/maven.sh -Dtest=AdminIntegracaoTests,PedidoIntegracaoTests,CompatibilidadeLegadoTests test
bash /workspace/.aurora/maven.sh verify
bash /workspace/.aurora/maven.sh -Dsurefire.runOrder=reversealphabetical test
JAVA_HOME=/workspace/.aurora/java-21 MAVEN_USER_HOME=/workspace/.m2 node scripts/test-cardapios.cjs
JAVA_HOME=/workspace/.aurora/java-21 MAVEN_USER_HOME=/workspace/.m2 node scripts/test-pedidos.cjs
sha256sum -c /workspace/.aurora/etapa4/h2-before.sha256
git diff --check
git push -u origin HEAD:refs/heads/etapa-4-revisao-testes-backend
```

Para outra instalação com JDK 21 e rede Maven configurada, os equivalentes são `./mvnw verify` e `./mvnw -Dsurefire.runOrder=reversealphabetical test`.

### Evidências desta instância

- `/workspace/.aurora/etapa4/baseline.log`: 55 aprovados.
- `admin.log`, `admin-recheck.log`: falhas de expectativa descritas acima, preservadas para rastreabilidade.
- `expanded.log`: grupo de 80 aprovado.
- `package.log`: pacote usado nos scripts de navegador, sem executar testes nesse comando.
- `final.log`: 109 aprovados e build.
- `final-repeat.log`: 111 aprovados e build final.
- `stability.log`: 111 aprovados em ordem inversa.
- `e2e-cardapios.log`, `e2e-pedidos.log`: resultados de navegador, código 0.
- `target/surefire-reports/`: XML/texto da execução mais recente; arquivos gerados e ignorados pelo Git.
- Evidências de navegador: `/tmp/aurora-functional-2vGOfk` e `/tmp/aurora-pedidos-2NGwJz` (bancos fictícios, logs e capturas temporários).

Esses caminhos são locais e podem não existir em outra máquina; os testes e o resumo dos resultados estão versionados. Não foi instalado plugin de cobertura e não se declara percentual de cobertura.

## Arquivos modificados

1. `src/test/java/br/com/cafeaurora/CafeAuroraApplicationTests.java`: banco próprio e perfil test.
2. `src/test/java/br/com/cafeaurora/PaginasCompartilhadasTests.java`: banco próprio e perfil test.
3. `src/test/java/br/com/cafeaurora/CardapioIntegracaoTests.java`: isolamento explícito e perfil test.
4. `src/test/java/br/com/cafeaurora/PedidoIntegracaoTests.java`: perfil test e sete cenários executados adicionais.
5. `src/test/java/br/com/cafeaurora/AdminIntegracaoTests.java`: novo grupo integrado administrativo e de segurança.
6. `src/test/java/br/com/cafeaurora/CompatibilidadeLegadoTests.java`: novo teste com esquema antigo e reinício em arquivo temporário.
7. `README.md`: correções técnicas pontuais e referência aos testes.
8. `docs/etapa-4-revisao-testes-backend.md`: este relatório.

## Limites e recomendações

Não restaram falhas técnicas nos comandos executados. O teste do esquema antigo usa fixture controlada; não comprova compatibilidade com toda variante de banco externo. Os scripts opcionais não foram executados com `LEGACY_H2_FILE` nem em modo exclusivamente visual móvel; houve captura móvel no fluxo normal de cardápios. Não houve teste manual de cada controle em todos os navegadores nem teste de carga prolongado. A matriz de status cobre operações sequenciais; concorrência entre dois administradores alterando o mesmo status permanece uma recomendação de aprofundamento, distinta da concorrência de criação/idempotência já exercitada.

Para evolução técnica: testar o banco de produção escolhido e introduzir migrações versionadas quando essa mudança entrar no escopo; avaliar concorrência de alterações administrativas e crescimento das cotações expiradas. Duplicidade de categoria sem distinguir caixa é verificada no serviço; a unicidade case-insensitive sob criações simultâneas merece teste específico antes de uso multioperador intensivo. São riscos residuais identificados por inspeção, não falhas reproduzidas e corrigidas nesta etapa.

Etapas futuras: manter avaliações/fotos, acompanhamento automático, revisão de entrega/retirada e eventual `EM_ENTREGA`, pagamentos e demais expansões nas respectivas Etapas 5–7, conforme planejamento da responsável. Nenhuma dessas funcionalidades foi implementada. Catálogo, checkout, acompanhamento, administração, segurança e aparência foram preservados. Encerrar na Etapa 4 e aguardar revisão.
