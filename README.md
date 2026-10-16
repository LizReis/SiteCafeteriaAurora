# Café Aurora ☕

Site de cafeteria desenvolvido para portfólio, integrando um frontend próprio (HTML, CSS e JavaScript) com **Spring Boot, Thymeleaf e H2**.

## Tecnologias
- Java 21 e Spring Boot 4.1.1
- Spring MVC + Thymeleaf: renderização das páginas
- Spring Data JPA + H2: base preparada para entidades e persistência
- HTML, CSS, JavaScript e jQuery: interface existente preservada
- Maven Wrapper: execução sem instalação global do Maven

## Executar localmente
**Pré-requisito:** JDK 21 instalado.

No terminal, na raiz do repositório:

**Windows (PowerShell):**
```powershell
.\mvnw.cmd spring-boot:run
```

**Linux/macOS:**
```bash
./mvnw spring-boot:run
```

Acesse **http://localhost:8080/**.

O Maven Wrapper baixa o Maven e as dependências na primeira execução (é preciso acesso à internet). Para executar os testes:

```bash
./mvnw test
```

### Console do H2 (somente em desenvolvimento)
Rode com o perfil `dev`:

**Windows (PowerShell):**
```powershell
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=dev"
```

**Linux/macOS:**
```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

Acesse **http://localhost:8080/h2-console**.

- JDBC URL: `jdbc:h2:file:./data/cafeaurora`
- User Name: `sa`
- Password: vazio

O banco fica na pasta `data/` e não é versionado. O perfil `dev` habilita o console H2 apenas para desenvolvimento local; não o habilite em uma publicação pública.

## Organização

```text
src/
  main/
    java/br/com/cafeaurora/
      CafeAuroraApplication.java
      controller/PaginaController.java
    resources/
      templates/                  # Páginas HTML processadas pelo Thymeleaf
      static/
        css/style.css
        js/scripts.js
        img/                      # Imagens originais do site
      application.properties
      application-dev.properties
  test/                           # Teste de inicialização
```

## Páginas

- `/` — inicial
- `/cardapio.html` — cardápio geral
- `/cardapio-cafe.html` — cafés
- `/cardapio-comidas.html` — comidas
- `/cardapio-drinks.html` — bebidas
- `/sacola-pedido.html` — sacola

Os endereços sem `.html` também são aceitos. Os links relativos, CSS, JavaScript e imagens foram mantidos.

## Estado atual e próximos passos

**Esta entrega é uma integração estrutural.** Os produtos exibidos nas páginas e os valores na sacola ainda são exemplos estáticos do frontend original. Botões de adicionar, remover e finalizar pedido, links `#` e cadastro de newsletter ainda não possuem backend. Não há pagamento real.

Evolução sugerida:
1. Modelar produtos, categorias e seus preços no H2 com JPA.
2. Renderizar cardápios dinamicamente usando Thymeleaf.
3. Implementar sacola funcional e cadastro/consulta de pedidos.
4. Criar painel administrativo e validações.
5. Escrever testes, otimizar imagens e preparar banco/segurança para implantação.

**Observação:** O H2 e `ddl-auto=update` são escolhas de desenvolvimento. Antes da publicação do projeto como aplicação com dados reais, configurar persistência de produção, controle de acesso e migrações de banco.
