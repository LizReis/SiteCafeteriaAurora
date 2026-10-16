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
$body = @{nome="Sobremesas"; descricao="Doces"; ativa=$true} | ConvertTo-Json
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
- Proteção CSRF é ignorada **somente** para `/api/admin/**`, que usa autenticação Basic sem cookies de sessão. Para um painel web com login/sessões, rever CSRF e a estratégia de autenticação.
- Console H2 está bloqueado pela configuração de segurança, inclusive no perfil dev, até definirmos uma política local segura para liberá-lo.
- O frontend continua com cardápio e sacola estáticos; endpoints de pedidos, painel administrativo HTML e pagamentos não estão implementados.
- Para implantar em produção, migrar H2 para um banco adequado, gerenciar segredos, criar migrações em vez de `ddl-auto=update` e implementar autenticação apropriada para usuários administradores.

