# TCG Backend — versão 2

API REST para lojas de card game organizarem torneios e eventos, e para jogadores se inscreverem, acompanharem a chave e colecionarem troféus.

**Stack:** Java 21 · Spring Boot 4.1 · Spring Data JPA (Hibernate) · Spring Security + JWT · MySQL 8 / H2

## Novidades da versão 2

**Regras do torneio**
- **Chave com bye:** a chave aceita qualquer número de confirmados (mínimo 2). Quem fica sem adversário avança direto. Antes, o número de confirmados precisava ser igual ao de vagas.
- **Avanço automático:** o vencedor vai para a próxima partida, a rodada encerra e a seguinte começa sozinha, e os jogadores são avisados quando a partida deles fica pronta.
- **Final encerra o torneio:** registrar o resultado da final finaliza o torneio e gera a classificação (1º, 2º, 3º...) e os troféus.
- **Status com regras:** o torneio só muda de status por caminhos válidos. `EM_ANDAMENTO` vem ao gerar a chave e `FINALIZADO`, ao registrar a final.
- **Desempate:** uma partida empatada fica aguardando e é decidida em `POST /partidas/{id}/desempate`.
- **Games e placar:** cadastrar games atualiza o placar da partida, e o placar precisa conferir com o resultado.
- **Prazos automáticos:** as inscrições fecham sozinhas em `inscricoesAte`, e os inscritos recebem um lembrete quando faltam menos de 24 h.

**Contas e segurança**
- **"Esqueci minha senha":** a senha pode ser redefinida com um código enviado por email.
- **Sessões:** existe logout, e trocar a senha encerra as sessões abertas em outros dispositivos.
- **Limite de login:** depois de 5 senhas erradas em 15 minutos, o login daquele email fica bloqueado (429).
- **Valores de desenvolvimento isolados:** chave JWT e senha do admin só têm valor padrão no perfil H2. O CORS é configurável, e o console do H2 só existe no perfil H2.

**Funcionários de loja**
- `TipoConta.FUNCIONARIO`: tipo de conta criado exclusivamente pelo **proprietário** da loja via `POST /lojas/{id}/funcionarios` com `{ email, senha, papel }`.
- O funcionário acessa o painel web normalmente; o `lojaId` vem no `LoginResponse` e é usado para rotear o acesso ao painel correto.
- Acesso ao app mobile: negado (o guard do web aceita `LOJA`, `ADMIN` e `FUNCIONARIO`).

**Check-in de partida**
- `POST /partidas/{id}/convocar` (equipe da loja): define `checkInExpiraEm = agora + 5 min` e envia notificação `CHECK_IN_SOLICITADO` aos dois jogadores.
- `POST /partidas/{id}/check-in` (jogador autenticado): registra `checkInAEm` ou `checkInBEm` conforme a inscrição do jogador.
- Os campos `checkInExpiraEm`, `checkInAEm` e `checkInBEm` retornam em `GET /partidas/{id}` para o app mobile fazer o polling.

**Mudança técnica:** o chaveamento e os resultados agora rodam em Java (`ChaveamentoService`), igual no H2 e no MySQL. As procedures do script SQL não são mais chamadas; as views e as triggers continuam valendo.

## Como rodar

**Com H2 (padrão, para testes).** Não precisa instalar banco nem configurar nada:

```bash
./mvnw spring-boot:run        # Windows: mvnw.cmd spring-boot:run
```

A API sobe em `http://localhost:8080`, já com dados de teste. Os dados voltam ao estado inicial a cada reinício.

**Com MySQL:**

1. Crie o banco com o script `TorneioTCG_SQL.sql`. Ele não está no repositório; peça à equipe.
   ```bash
   mysql -u root -p < TorneioTCG_SQL.sql
   ```
2. Defina pelo menos `JWT_SECRET` e, na primeira execução, `ADMIN_SENHA`, e suba com o perfil `mysql`:
   ```bash
   export JWT_SECRET="uma-chave-longa-com-pelo-menos-32-caracteres"
   export ADMIN_SENHA="senha-do-admin"
   ./mvnw spring-boot:run -Dspring-boot.run.profiles=mysql
   ```

> O perfil `mysql` só roda com esse script, que precisa entrar no repositório. Ao atualizá-lo, a view `vw_trofeus` deve ignorar torneios excluídos (`deletado_em`), como a versão do H2 em `resources/h2/schema.sql`.

> Erro `Connection refused` ao subir = o MySQL não está rodando. Erro `Defina a variável de ambiente JWT_SECRET` = falta a chave JWT.

### Variáveis de ambiente

| Variável | Para que serve | Padrão |
|---|---|---|
| `JWT_SECRET` | Chave dos tokens (mínimo 32 caracteres) | Obrigatória fora do H2 |
| `ADMIN_EMAIL` / `ADMIN_SENHA` | Admin criado na primeira execução | `admin@tcg.com` / sem senha fora do H2 (o admin não é criado) |
| `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` | Conexão com o MySQL | `localhost:3306`, `root` / `root` |
| `CORS_ORIGENS` | Endereços do front que podem chamar a API, separados por vírgula | `http://localhost:*,http://127.0.0.1:*` |
| `SPRING_MAIL_HOST`, `SPRING_MAIL_PORT`, `SPRING_MAIL_USERNAME`, `SPRING_MAIL_PASSWORD`, `MAIL_FROM` | SMTP para enviar o email de redefinição de senha | Sem SMTP, o email é escrito no log |
| `JWT_EXPIRACAO_MS` | Validade do login | 24 h |
| `AGENDADOR_INTERVALO_MS` | Intervalo das tarefas automáticas | 1 minuto |
| `UPLOAD_DIR` | Pasta das imagens enviadas (foto de perfil), relativa à pasta em que a API é iniciada | `uploads` |

## Contas de teste (H2)

| Email | Senha | Tipo |
|---|---|---|
| `admin@tcg.com` | `admin123` | Admin |
| `contato@cardhouse.com.br` | `123456` | Loja — Card House (conta_id 1) |
| `contato@dragonslair.com.br` | `123456` | Loja — Dragon's Lair (conta_id 6) |
| `eric@email.com`, `samuel@email.com`, `vinicius@email.com`, `lucas@email.com` | `123456` | Jogadores (ids 2 a 5) |

O torneio 1 já tem 4 inscritos confirmados, pronto para gerar a chave. No MySQL, só o admin funciona: as senhas das contas de exemplo do script são fictícias.

> **Atenção (H2):** `loja_membro.loja_id`, `torneio.loja_id` e `evento.loja_id` referenciam `loja.conta_id`, não `loja.id`. O `conta_id` da Dragon's Lair é 6, portanto todas as linhas de teste usam `loja_id=6`.

**H2 Console:** `http://localhost:8080/h2-console`, com JDBC URL `jdbc:h2:mem:tcg_torneios`, usuário `sa` e senha vazia.

## Endpoints

- **Consultas (`GET`):** públicas, menos `/inscricoes` (o próprio jogador, a equipe da loja ou um admin), `/notificacoes`, `/contas`, `/loja-membros`, `/administradores` e `/jogadores/me`, que exigem token.
- **Criar, editar e excluir:** exigem o header `Authorization: Bearer <token>`. O token vem de `POST /auth/login`.
- **Rotas padrão:** todos os recursos têm `GET /recurso`, `GET /recurso/{id}`, `POST`, `PUT /{id}` e `DELETE /{id}`.
- **Corpo das requisições:** os campos estão nos records de `dto/`.
- **Datas:** formato `2026-12-20T19:00:00`.

| Rota | Filtros (`GET`) | Rotas extras | Quem pode alterar |
|---|---|---|---|
| `/auth` | | `POST /login`, `POST /registro/jogador`, `POST /registro/loja`, `GET /me`, `POST /logout`, `POST /esqueci-senha`, `POST /redefinir-senha` | Público (exceto `me` e `logout`) |
| `/jogos`, `/formatos` | `ativo`, `jogoId` | | Admin |
| `/lojas` | | `GET /slug/{slug}`, `GET /{id}/agenda`, `PUT /{id}/verificacao` (admin) | Dono da loja |
| `/loja-membros` | `lojaId` | | Dono da loja |
| `/jogadores` | | `GET /me`, `GET /nickname/{nick}`, `GET /{id}/trofeus`, `POST /{id}/imagem` | O próprio jogador |
| `/enderecos` | | | Lojas e admin |
| `/eventos` | `lojaId`, `status` | | Equipe da loja |
| `/evento-participacoes` | `eventoId`, `jogadorId` | | Jogador ou equipe da loja |
| `/torneios` | `lojaId`, `jogoId`, `status` (aceita vários: `?status=A,B`) | `PUT /{id}/status`, `POST` e `GET /{id}/chaveamento`, `GET /{id}/vagas` | Equipe da loja |
| `/inscricoes` | `jogadorId` (as suas) ou `torneioId` (equipe da loja), `status` | `PUT /{id}/check-in`, `PUT /{id}/cancelar` | Jogador ou equipe da loja |
| `/rodadas`, `/partidas`, `/games` | `torneioId`, `rodadaId`, `partidaId` | `POST /partidas/{id}/resultado`, `POST /partidas/{id}/desempate`, `POST /partidas/{id}/reabrir`, `POST /partidas/{id}/convocar`, `POST /partidas/{id}/check-in` | Equipe da loja (convocar); jogador logado (check-in) |
| `/lojas/{id}/funcionarios` | — | `POST` | Proprietário da loja |
| `/torneio-resultados` | `torneioId`, `jogadorId` | | Equipe da loja |
| `/notificacoes` | só as suas | `GET /nao-lidas`, `GET /nao-lidas/total`, `PUT /{id}/lida`, `PUT /lidas` | O próprio usuário |
| `/contas` | `tipo` | `PUT /{id}/senha`, `PUT /{id}/status` (admin) | O próprio usuário ou admin |
| `/administradores` | | | Admin |

**Equipe da loja** = a conta da loja, qualquer membro ativo dela (proprietário, organizador, juiz ou **funcionário**) ou um admin.

Excluir conta, torneio ou evento é *soft delete*: o registro fica no banco, mas some da API.

## Fluxo de um torneio

1. **Criar:** `POST /torneios` (nasce em `RASCUNHO`). Depois, `PUT /torneios/{id}/status` com `INSCRICOES_ABERTAS`.
2. **Inscrever:** os jogadores fazem `POST /inscricoes` com `{ "torneioId" }`. Quem passar das vagas vai para `LISTA_ESPERA` e sobe sozinho se alguém cancelar ou se a loja aumentar as vagas.
3. **Check-in:** no dia, `PUT /inscricoes/{id}/check-in` muda a inscrição para `CONFIRMADO`.
4. **Chave:** com as inscrições encerradas (por `PUT /status` ou automaticamente no prazo), `POST /torneios/{id}/chaveamento` sorteia os confirmados. A partir daqui as inscrições não mudam mais de status (nem check-in).
5. **Resultados:** `POST /partidas/{id}/resultado` com `{ "gamesA", "gamesB", "resultado" }` (os games podem vir de `/games`). Em caso de `EMPATE`, `POST /partidas/{id}/desempate` com `{ "vencedor": "A" }` ou `"B"`. A final precisa de um vencedor: não aceita `DUPLO_NO_SHOW`.
6. **Corrigir um resultado:** `POST /partidas/{id}/reabrir` desfaz o resultado (tira o vencedor da partida seguinte e desfaz o W.O.) enquanto a partida seguinte não começou. Depois, ajuste os games e registre o resultado de novo.
7. **Fim:** o resultado da final finaliza o torneio e gera a classificação em `/torneio-resultados`. A loja só completa o prêmio, se quiser. Os troféus aparecem em `GET /jogadores/{id}/trofeus`.

Na chave gerada automaticamente, rodadas e partidas não podem ser criadas, apagadas nem religadas à mão, e `PUT /partidas/{id}` só muda mesa e observação. O CRUD completo de rodadas e partidas vale para torneios montados manualmente.

Status do torneio: `RASCUNHO` ⇄ `INSCRICOES_ABERTAS` ⇄ `INSCRICOES_ENCERRADAS` → `EM_ANDAMENTO` → `FINALIZADO`. Qualquer status antes do fim pode ir para `CANCELADO`.

## Integração com o app mobile

O app (TCG-front-mobile) é só para jogadores. O que a API oferece para ele:

- **Login e cadastro** (`POST /auth/login`, `POST /auth/registro/jogador`) devolvem, além do token, `nome`, `nickname` e `imagemPerfil`, para o app montar a sessão sem outra chamada.
- **`GET /jogadores/me`:** perfil completo do jogador logado, com email e data de nascimento (que não saem no perfil público). Tem os mesmos campos do `PUT /jogadores/{id}`, que substitui o perfil inteiro.
- **Foto de perfil:** `POST /jogadores/{id}/imagem` (multipart, campo `arquivo`, JPG/PNG/WEBP até 5 MB). O arquivo fica na pasta `UPLOAD_DIR` e o banco guarda o caminho `/uploads/jogadores/<arquivo>`, servido em `GET /uploads/**`. O app completa o caminho com o endereço da API.
- **Torneios** trazem `vagasDisponiveis` e `vagasOcupadas`, sem precisar de `GET /{id}/vagas` para cada um. A vitrine do app usa `?status=INSCRICOES_ABERTAS,INSCRICOES_ENCERRADAS,EM_ANDAMENTO,FINALIZADO`.
- **Privacidade:** o status de pagamento só aparece em `/inscricoes` (para o dono e a equipe da loja), e a data de nascimento só em `/jogadores/me`.

## Contas e segurança

- **Esqueci minha senha:** `POST /auth/esqueci-senha` envia por email um código para `POST /auth/redefinir-senha`. O código vale 30 minutos e uma única vez. Sem SMTP configurado, o email aparece no log da aplicação.
- **Tokens:** trocar a senha devolve um token novo e invalida os anteriores. `POST /auth/logout` encerra só o token atual.
- **Limites:** 5 logins errados em 15 minutos bloqueiam o email (429). Os pedidos de "esqueci minha senha" também são limitados.
- **Limites:** login de conta desativada com senha errada responde 401, como qualquer senha errada (não revela que o email existe).
- **Limitação:** a lista de logouts, a contagem de tentativas e o controle de lembretes já enviados ficam na memória da aplicação. Reiniciar a aplicação os zera, e com mais de uma instância rodando, cada uma teria o seu.

## Erros

As respostas de erro vêm em JSON, com a mensagem no campo `detail`:

| Status | Significado |
|---|---|
| 400 | Dados inválidos, placar que não confere ou código inválido |
| 401 | Sem token, token inválido/encerrado ou login errado |
| 403 | Sem permissão ou conta desativada |
| 404 | Não encontrado |
| 409 | Conflito: registro duplicado, torneio finalizado, sem vagas, mudança de status inválida ou regra barrada pelo banco |
| 429 | Muitas tentativas seguidas |

## Estrutura

```
src/main/java/senac/com/backendTCG
├── config/       criação do admin
├── controller/   rotas REST
├── dto/          corpos de requisição e resposta
├── entity/       tabelas do banco (+ enums)
├── exception/    tratamento de erros
├── repository/   acesso ao banco
├── security/     JWT, logout, limite de tentativas e permissões de rota
└── service/      regras de negócio, chaveamento, email de senha e agendador
```

Perfis: `application-h2.properties` e `application-mysql.properties`. Os scripts do H2 ficam em `resources/h2/`.
