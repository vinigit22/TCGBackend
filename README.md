# TCG Backend

API REST para lojas de card game organizarem torneios e eventos, e para jogadores se inscreverem, acompanharem a chave e colecionarem troféus.

**Stack:** Java 21 · Spring Boot 4.1 · Spring Data JPA (Hibernate) · Spring Security + JWT · MySQL 8 / H2

## Como rodar

**Com H2 (padrão, para testes).** Não precisa instalar banco:

```bash
./mvnw spring-boot:run        # Windows: mvnw.cmd spring-boot:run
```

A API sobe em `http://localhost:8080`, já com dados de teste. Os dados voltam ao estado inicial a cada reinício.

**Com MySQL:**

1. Crie o banco com o script `TorneioTCG_SQL.sql`. Ele não está no repositório; peça à equipe.
   ```bash
   mysql -u root -p < TorneioTCG_SQL.sql
   ```
2. Suba com o perfil `mysql`:
   ```bash
   ./mvnw spring-boot:run -Dspring-boot.run.profiles=mysql
   ```
   A conexão padrão é `root` / `root` em `localhost:3306`. Para mudar, use as variáveis `DB_URL`, `DB_USERNAME` e `DB_PASSWORD`.

> Erro `Connection refused` ao subir = o perfil `mysql` está ativo, mas o MySQL não está rodando.

## Contas de teste (H2)

| Email | Senha | Tipo |
|---|---|---|
| `admin@tcg.com` | `admin123` | Admin |
| `contato@cardhouse.com.br` | `123456` | Loja (id 1) |
| `eric@email.com`, `samuel@email.com`, `vinicius@email.com`, `lucas@email.com` | `123456` | Jogadores (ids 2 a 5) |

O torneio 1 já tem 4 inscritos confirmados, pronto para gerar a chave.

No MySQL, só o admin funciona: as senhas das contas de exemplo do script são fictícias.

**H2 Console:** `http://localhost:8080/h2-console`, com JDBC URL `jdbc:h2:mem:tcg_torneios`, usuário `sa` e senha vazia.

## Endpoints

- **Consultas (`GET`):** públicas.
- **Criar, editar e excluir:** exigem o header `Authorization: Bearer <token>`. O token vem de `POST /auth/login`.
- **Rotas padrão:** todos os recursos têm `GET /recurso`, `GET /recurso/{id}`, `POST`, `PUT /{id}` e `DELETE /{id}`.
- **Corpo das requisições:** os campos estão nos records de `dto/`.
- **Datas:** formato `2026-12-20T19:00:00`.

| Rota | Filtros (`GET`) | Rotas extras | Quem pode alterar |
|---|---|---|---|
| `/auth` | | `POST /login`, `POST /registro/jogador`, `POST /registro/loja`, `GET /me` | Público |
| `/jogos`, `/formatos` | `ativo`, `jogoId` | | Admin |
| `/lojas` | | `GET /slug/{slug}`, `GET /{id}/agenda`, `PUT /{id}/verificacao` (admin) | Dono da loja |
| `/loja-membros` | `lojaId` | | Dono da loja |
| `/jogadores` | | `GET /nickname/{nick}`, `GET /{id}/trofeus` | O próprio jogador |
| `/enderecos` | | | Lojas e admin |
| `/eventos` | `lojaId`, `status` | | Equipe da loja |
| `/evento-participacoes` | `eventoId`, `jogadorId` | | Jogador ou equipe da loja |
| `/torneios` | `lojaId`, `jogoId`, `status` | `PUT /{id}/status`, `POST` e `GET /{id}/chaveamento`, `GET /{id}/vagas` | Equipe da loja |
| `/inscricoes` | `torneioId`, `jogadorId`, `status` | `PUT /{id}/check-in`, `PUT /{id}/cancelar` | Jogador ou equipe da loja |
| `/rodadas`, `/partidas`, `/games` | `torneioId`, `rodadaId`, `partidaId` | `POST /partidas/{id}/resultado` | Equipe da loja |
| `/torneio-resultados` | `torneioId`, `jogadorId` | | Equipe da loja |
| `/notificacoes` | só as suas | `GET /nao-lidas`, `GET /nao-lidas/total`, `PUT /{id}/lida`, `PUT /lidas` | O próprio usuário |
| `/contas` | `tipo` | `PUT /{id}/senha`, `PUT /{id}/status` (admin) | O próprio usuário ou admin |
| `/administradores` | | | Admin |

**Equipe da loja** = a conta da loja, qualquer membro ativo dela (proprietário, organizador ou juiz) ou um admin.

Excluir conta, torneio ou evento é *soft delete*: o registro fica no banco, mas some da API.

## Fluxo de um torneio

1. **Criar:** `POST /torneios` (nasce em `RASCUNHO`). Depois, `PUT /torneios/{id}/status` com `INSCRICOES_ABERTAS`.
2. **Inscrever:** os jogadores fazem `POST /inscricoes` com `{ "torneioId" }`. Quem passar das vagas vai para `LISTA_ESPERA`.
3. **Check-in:** no dia, `PUT /inscricoes/{id}/check-in` muda a inscrição para `CONFIRMADO`.
4. **Chave:** status `INSCRICOES_ENCERRADAS` e depois `POST /torneios/{id}/chaveamento`. O número de confirmados precisa ser igual ao de vagas.
5. **Resultados:** `POST /partidas/{id}/resultado` com `{ "gamesA", "gamesB", "resultado" }`. O vencedor avança sozinho na chave.
6. **Encerrar:** status `FINALIZADO` (a partir daí o torneio não pode mais ser editado) e `POST /torneio-resultados` para a classificação, que aparece em `GET /jogadores/{id}/trofeus`.

## Erros

As respostas de erro vêm em JSON, com a mensagem no campo `detail`:

| Status | Significado |
|---|---|
| 400 | Dados inválidos |
| 401 | Sem token, token inválido ou login errado |
| 403 | Sem permissão |
| 404 | Não encontrado |
| 409 | Conflito: registro duplicado, torneio finalizado, sem vagas ou regra barrada pelo banco |

## Estrutura

```
src/main/java/senac/com/backendTCG
├── config/       criação do admin e procedures do H2
├── controller/   rotas REST
├── dto/          corpos de requisição e resposta
├── entity/       tabelas do banco (+ enums)
├── exception/    tratamento de erros
├── repository/   acesso ao banco
├── security/     JWT e permissões de rota
└── service/      regras de negócio
```

Perfis: `application-h2.properties` e `application-mysql.properties`. Os scripts do H2 ficam em `resources/h2/`.
