# Café Aurora ☕

Projeto de portfólio em Java 21, Spring Boot 4.1.1, Thymeleaf, H2 e frontend HTML/CSS/JS.

## Execução (Windows / PowerShell)

```powershell
git pull origin main
$env:CAFE_ADMIN_PASSWORD = "EscolhaUmaSenhaForte"
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=dev"
```

Acesse `http://localhost:8080`. Os 9 produtos de demonstração são inseridos no perfil `dev` apenas se o banco estiver vazio.

Para testar: `./mvnw test` (no Windows: `.\mvnw.cmd test`).

### Página de localização

- `/localizacao` ou `/localizacao.html` — página com banner, endereço ilustrativo e mapa incorporado.
- O menu **Localização** é definido uma única vez em `templates/fragments/layout.html`, compartilhado por todas as páginas.
- Foto: `static/img/localizacao-img.png`. Estilo independente: `static/css/localizacao.css`.
- **Demonstração:** o endereço do MASP é um ponto público de referência, não uma unidade do Café Aurora. Horários são fictícios e deverão ser substituídos.
- O mapa utiliza um iframe de consulta do Google Maps (`?output=embed`, sem chave). Essa opção não é a Maps Embed API oficial; para produção, gere o código de incorporação pelo Google Maps ou utilize a Maps Embed API com chave, se necessário.

### API de catálogo (pública)
- `GET /api/categorias`
- `GET /api/produtos` (opcional `?categoriaId=1`)
- `GET /api/produtos/{id}`

### CRUD administrativo (autenticação HTTP Basic, ROLE_ADMIN)
- `GET /api/admin/categorias` e `GET /api/admin/categorias/{id}`
- `POST /api/admin/categorias`
- `PUT /api/admin/categorias/{id}`
- `DELETE /api/admin/categorias/{id}` (desativação)
- `GET /api/admin/produtos` e `GET /api/admin/produtos/{id}`
- `POST /api/admin/produtos`
- `PUT /api/admin/produtos/{id}`
- `DELETE /api/admin/produtos/{id}` (desativação)

Exemplo de criação de categoria (PowerShell):

```powershell
$cred = Get-Credential -UserName admin
$body = @{nome="Sobremesas"; descricao="Doces"; ativa=$true; tipoCardapio="COMIDAS"; ordemExibicao=0} | ConvertTo-Json
Invoke-RestMethod -Uri "http://localhost:8080/api/admin/categorias" -Method Post -Credential $cred -Authentication Basic -ContentType "application/json" -Body $body
```

Exemplo de criação de produto (troque `categoriaId` pelo ID real):
```json
{
  "nome": "Bolo de Cenoura",
  "descricao": "Com cobertura de chocolate",
  "preco": 15.50,
  "imagemUrl": null,
  "categoriaId": 1,
  "ativo": true,
  "disponivel": true
}
```

Use `POST /api/admin/produtos` com o corpo JSON acima e credenciais Basic.

**Respostas:** 201 ao criar; 200 para consulta/edição; 204 ao desativar; 400 para dados inválidos; 401 para falta de autenticação; 403 para permissão negada; 404 para ID inexistente; 409 para conflito de categoria duplicada ou tentativa de ativar produto em categoria inativa.

### Organização, SOLID e padrões

- **Controller–Service–Repository:** controllers processam HTTP, services concentram regras e repositórios persistem dados (separação de responsabilidades / SRP).
- **Dependency Injection:** construtores recebem dependências gerenciadas pelo Spring; evita instanciar colaboradores diretamente (DIP, com possibilidade de evoluir services para interfaces).
- **Repository Pattern:** Spring Data JPA isola acesso a dados.
- **DTO Pattern:** `ProdutoRequest/Response` e `CategoriaRequest/Response` isolam a API do modelo JPA; Bean Validation impede entradas inválidas.
- **Global Exception Handler:** tratamento uniforme dos erros sem duplicar esse código em controllers.
- **Transações:** métodos de serviço usam `@Transactional` para alterações atômicas.
- **Soft delete:** DELETE desativa entidades para preservar referência/histórico, sem apagar linhas.

Não é correto afirmar que todos os princípios SOLID estão integralmente atendidos: OCP e ISP ainda não são particularmente relevantes a este escopo; serviços concretos injetados podem futuramente evoluir para portas/interfaces quando houver necessidade real.

### Segurança e limites
- API `/api/admin/**` requer HTTP Basic. Usuário padrão é `admin` (configurável por `CAFE_ADMIN_USERNAME`) e senha deve ser fornecida por `CAFE_ADMIN_PASSWORD`. **O servidor não inicializa sem a senha definida.**
- Senhas não são salvas em arquivos versionados. HTTP Basic exige HTTPS fora do ambiente local.
- Proteção CSRF é ignorada **somente** para `/api/admin/**`, que usa autenticação Basic sem cookies de sessão. O painel HTML usa login por formulário, sessão e CSRF ativo.
- Console H2 está bloqueado pela configuração de segurança, inclusive no perfil dev, até definirmos uma política local segura para liberá-lo.
- Os cardápios consultam H2 e a sacola permanece local; o backend de pedidos está implementado (Etapa 2); painel administrativo HTML implementado (Etapa 3); processamento de pagamentos ainda não implementado.
- Para implantar em produção, migrar H2 para um banco adequado, gerenciar segredos, criar migrações em vez de `ddl-auto=update` e implementar autenticação apropriada para usuários administradores.


## Cardápios integrados ao H2 — Etapa 1

Os três cardápios renderizam produtos e seções do banco. Categorias usam `tipoCardapio` (`CAFE`, `DRINKS`, `COMIDAS`) e `ordemExibicao` (inteiro não negativo), obrigatórios no POST/PUT administrativo. O nome permanece editável.

Consulte [a documentação da Etapa 1](docs/etapa-1-cardapios.md) para arquitetura, compatibilidade com bancos anteriores, contrato da API, sacola e testes. Categorias legadas personalizadas sem classificação devem receber um tipo pelo CRUD para aparecer nas páginas. Não apague o banco existente.

## Pedidos persistidos — Etapa 2

O checkout solicita uma cotação oficial, registra pedidos/itens no H2 com CSRF e idempotência e consulta a confirmação por referência aleatória. O pagamento é apenas intenção (`PENDENTE`), sem gateway ou coleta de dados de cartão. Consulte [contratos, imagens, compatibilidade e testes](docs/etapa-2-pedidos.md). A taxa oficial é configurada por `cafe.pedido.taxa-entrega` (padrão `7.00`; retirada sem taxa).

## Área administrativa — Etapa 3

Após configurar CAFE_ADMIN_PASSWORD e iniciar o projeto, visite http://localhost:8080/admin/login.
Entre com CAFE_ADMIN_USERNAME (padrão: admin) e a senha configurada no ambiente.

- /admin: indicadores e últimos pedidos;
- /admin/categorias: criar, editar e desativar categorias;
- /admin/produtos: criar, editar e desativar produtos, incluindo preço, categoria, imagem existente em /img e disponibilidade;
- /admin/pedidos: filtrar e paginar pedidos;
- /admin/pedidos/{id}: consultar detalhes e avançar status.

Fluxo de status permitido: RECEBIDO → EM_PREPARO → PRONTO → CONCLUIDO, com CANCELADO permitido antes da conclusão. Estados finais não podem ser reabertos. PRONTO indica pedido pronto; para retirada, pronto para retirar; não existe enum EM_ENTREGA ainda.

O painel usa autenticação por formulário e sessão com proteção CSRF. A API /api/admin/** preserva HTTP Basic stateless. Não exponha credenciais nem utilize HTTP sem TLS fora do desenvolvimento local. A desativação não exclui dados históricos.

**Limites:** O painel usa caminhos para imagens já existentes em static/img (não há upload). O pagamento online não foi implementado. A configuração atual permanece orientada a ambiente local H2, não a produção.

Execute `.\\mvnw.cmd test` e `.\\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=dev"` (PowerShell) para validação local. Teste também os formulários HTML, login/logout, CSRF e a consulta pública dos pedidos após mudança de status.

## Auditoria e testes — Etapa 4

Execute `./mvnw verify` com JDK 21. Os testes usam H2 em memória por classe; o teste de compatibilidade cria um arquivo exclusivamente temporário. Não execute testes apontando para `data/cafeaurora`. Consulte [o relatório da Etapa 4](docs/etapa-4-revisao-testes-backend.md) para cenários, resultados e comandos do ambiente na nuvem.

## Avaliações reais — Etapa 5

Pedidos `CONCLUIDO` podem receber uma avaliação, mesmo com pagamento `PENDENTE`. Na tela de acompanhamento, informe nome de exibição, nota, comentário e consentimento; a foto JPEG/PNG/WebP é opcional (até 5 MB). O backend impede reenvio por pedido.

As avaliações aguardam revisão em `/admin/avaliacoes`. Somente as aprovadas aparecem na Home (cinco mais recentes). Aprovação e rejeição exigem login administrativo e CSRF; fotos pendentes/rejeitadas não são públicas. Imagens são validadas e normalizadas para JPEG, armazenadas no H2 em tabela separada.

Execute `./mvnw verify` com JDK 21. O teste de navegador `scripts/test-avaliacoes.cjs` exige pacote Playwright e Chromium, JAR gerado por `./mvnw package`, `JAVA_HOME` apontando para JDK 21 e porta local 18083 livre. Use `JAVA_HOME=/caminho/do/jdk node scripts/test-avaliacoes.cjs` (ajuste `CHROMIUM_PATH` se necessário). Ele usa somente banco e aplicativo temporários. Consulte [contratos, segurança e evidências](docs/etapa-5-avaliacoes-reais.md).
