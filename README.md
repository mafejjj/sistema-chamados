# Sistema de Chamados

API REST para cadastro e acompanhamento de chamados.

## Como executar

```powershell
cmd /c mvnw.cmd spring-boot:run
```

A aplicação inicia em `http://localhost:8080`. O banco usado no desenvolvimento é H2 em memória.

## Rotas principais

### Clientes

- `POST /clientes`
- `GET /clientes`
- `GET /clientes/{id}`
- `PUT /clientes/{id}`
- `DELETE /clientes/{id}`

Exemplo de criação:

```json
{"nome":"Ana Souza","email":"ana@exemplo.com","telefone":"47999999999"}
```

### Categorias

- `POST /categorias`
- `GET /categorias`
- `GET /categorias/{id}`
- `PUT /categorias/{id}`
- `DELETE /categorias/{id}`

Exemplo:

```json
{"nome":"Software","descricao":"Problemas em sistemas e aplicativos"}
```

### Técnicos

- `POST /tecnicos`
- `GET /tecnicos`
- `GET /tecnicos/{id}`
- `PUT /tecnicos/{id}`
- `DELETE /tecnicos/{id}`

Exemplo:

```json
{"nome":"Carlos Lima","email":"carlos@exemplo.com","perfil":"TECNICO"}
```

Os perfis aceitos são `ADMIN` e `TECNICO`.

### Chamados

- `POST /chamados`
- `GET /chamados?status=ABERTO&prioridade=ALTA&clienteId=1&page=0&size=20&sort=dataAbertura,desc`
- `GET /chamados/{id}`
- `PUT /chamados/{id}`
- `DELETE /chamados/{id}`
- `PATCH /chamados/{id}/atribuir`
- `PATCH /chamados/{id}/status`
- `GET /clientes/{id}/chamados`

Exemplo de criação:

```json
{
	"titulo":"Erro no sistema",
	"descricao":"Tela indisponível para o cliente",
	"prioridade":"ALTA",
	"clienteId":1,
	"categoriaId":1
}
```

Um chamado nasce com status `ABERTO` e data de abertura automática. As transições aceitas são:

- `ABERTO` para `EM_ATENDIMENTO`;
- `EM_ATENDIMENTO` para `AGUARDANDO_CLIENTE` ou `FECHADO`;
- `AGUARDANDO_CLIENTE` para `EM_ATENDIMENTO` ou `FECHADO`.

Ao fechar, a data de fechamento é preenchida. Chamados fechados não podem ser alterados.

### Comentários

- `POST /chamados/{id}/comentarios`
- `GET /chamados/{id}/comentarios`

Exemplo:

```json
{"texto":"Estamos analisando o problema","autor":"Carlos Lima"}
```

## Testes

```powershell
cmd /c mvnw.cmd test
```

A suíte cobre o checkpoint do fluxo Cliente, CRUD de Categorias e Técnicos,
criação e regras de Chamados, filtros com paginação, comentários, validações,
duplicidade e recursos inexistentes. A entrega deve ser validada pelo professor
antes do commit e do Pull Request.

O console do H2 fica disponível em `http://localhost:8080/h2-console`, usando JDBC URL `jdbc:h2:mem:chamadosdb`, usuário `sa` e senha em branco.