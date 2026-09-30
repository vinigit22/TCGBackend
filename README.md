# TCG Backend — API de torneios de card game

API REST em Spring Boot para lojas de card game organizarem **torneios** e **eventos**, e para **jogadores** se inscreverem, acompanharem a chave e colecionarem troféus.

O banco é definido pelo script `TorneioTCG_SQL.sql`, que **não fica neste repositório** (é mantido à parte pela equipe). A API foi construída em cima dele: cada tabela tem entidade, repositório, service e controller com CRUD, e as triggers, views e procedures do script são usadas pelos endpoints.

Para testes, a aplicação também roda **temporariamente com o H2**, um banco em memória que não precisa de instalação. Hoje ele é o padrão (veja [Usando o H2](#usando-o-h2-testes)).

## Sumário

1. [Stack](#stack)
2. [Como rodar](#como-rodar)
3. [Usando o H2 (testes)](#usando-o-h2-testes)
4. [Problemas comuns ao rodar](#problemas-comuns-ao-rodar)
5. [O que foi feito nesta entrega](#o-que-foi-feito-nesta-entrega)
6. [Estrutura do projeto](#estrutura-do-projeto)
7. [Autenticação](#autenticação)
8. [Permissões](#permissões)
9. [Endpoints](#endpoints)
10. [Integração com triggers, views e procedures](#integração-com-triggers-views-e-procedures)
11. [Regras de negócio](#regras-de-negócio)
12. [Exemplo: um torneio do início ao fim](#exemplo-um-torneio-do-início-ao-fim)
13. [Formato de erros](#formato-de-erros)
14. [Como foi testado](#como-foi-testado)
15. [Observações e próximos passos](#observações-e-próximos-passos)

---

## Stack

| Item | Tecnologia |
|---|---|
| Linguagem | Java 21 |
| Framework | Spring Boot 4.1.1 (Web MVC, Data JPA, Security, Validation) |
| Banco | MySQL 8 (schema, triggers, views e procedures vêm do `TorneioTCG_SQL.sql`) e, temporariamente para testes, H2 2.4 em memória |
| Autenticação | JWT (jjwt 0.13) + senhas com BCrypt |
| Outros | Lombok |

---

## Como rodar

**Pré-requisito:** JDK 21 ou superior. O MySQL só é necessário para o perfil `mysql`.

O banco é escolhido pelo **perfil** do Spring:

| Perfil | Banco | Quando usar |
|---|---|---|
| `h2` (**padrão**, temporário) | H2 em memória, dentro da própria aplicação | Testes e desenvolvimento. Não precisa instalar nada. |
| `mysql` | MySQL 8 | Banco real, criado pelo `TorneioTCG_SQL.sql` |

Se nenhum perfil for informado, a aplicação sobe com o **H2**.

### Com H2 (padrão)

```bash
./mvnw spring-boot:run        # Linux / macOS / Git Bash
mvnw.cmd spring-boot:run      # Windows (cmd / PowerShell)
```

Ou execute a classe `BackendTcgApplication` pelo botão **Run** do VS Code ou do IntelliJ. O passo a passo completo está em [Usando o H2 (testes)](#usando-o-h2-testes).

### Com MySQL

**1. Crie o banco** com o script `TorneioTCG_SQL.sql`. Ele **não está no repositório**: peça o arquivo à equipe. O script cria o banco `tcg_torneios` com tabelas, triggers, views, procedures e dados de teste:

```bash
mysql -u root -p < caminho/para/TorneioTCG_SQL.sql
```

> Esse passo é obrigatório. No perfil `mysql` o Hibernate não cria tabelas (`ddl-auto=none`), então sem o script a aplicação não sobe.

**2. Configure a conexão.** O padrão é `root` / `root` em `localhost:3306`. Para mudar, use variáveis de ambiente (ou edite `src/main/resources/application-mysql.properties`):

| Variável | Padrão | Para que serve |
|---|---|---|
| `DB_URL` | `jdbc:mysql://localhost:3306/tcg_torneios?useSSL=false&allowPublicKeyRetrieval=true` | URL JDBC |
| `DB_USERNAME` | `root` | Usuário do MySQL |
| `DB_PASSWORD` | `root` | Senha do MySQL |

**3. Suba com o perfil `mysql`:**

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=mysql      # Linux / macOS / Git Bash
mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=mysql    # Windows
java -jar target/backendTCG-0.0.1-SNAPSHOT.jar --spring.profiles.active=mysql   # pelo .jar
```

Na IDE, defina a variável de ambiente `SPRING_PROFILES_ACTIVE=mysql` na configuração de execução.

> **Atenção:** as contas de exemplo do script (`eric@email.com`, `contato@cardhouse.com.br` etc.) usam hashes BCrypt fictícios e **não conseguem fazer login no MySQL**. Use o admin ou cadastre novas contas pela API.

### Configurações comuns aos dois perfis

| Variável | Padrão | Para que serve |
|---|---|---|
| `JWT_SECRET` | chave de desenvolvimento | Troque em produção (mínimo de 32 caracteres) |
| `JWT_EXPIRACAO_MS` | `86400000` (24 h) | Validade do token |
| `ADMIN_EMAIL` / `ADMIN_SENHA` | `admin@tcg.com` / `admin123` | Administrador criado na primeira execução |

A API fica em `http://localhost:8080`. Na primeira execução o `AdminInitializer` cria o administrador **admin@tcg.com / admin123**.

---

## Usando o H2 (testes)

> **Temporário.** O H2 serve para testar a API sem instalar o MySQL. O banco oficial continua sendo o MySQL com o `TorneioTCG_SQL.sql`.

O H2 é um banco de dados escrito em Java que roda **dentro da própria aplicação**, em memória. Não há instalação nem configuração: ele é criado quando a API sobe e apagado quando ela para.

### 1. Suba a aplicação

```bash
./mvnw spring-boot:run        # ou mvnw.cmd no Windows, ou o botão Run da IDE
```

No log devem aparecer estas linhas:

```
No active profile set, falling back to 1 default profile: "h2"
H2 console available at '/h2-console'. Database available at 'jdbc:h2:mem:tcg_torneios'
ADMIN CRIADO: admin@tcg.com
```

### 2. O que já vem pronto

A cada inicialização acontece o seguinte, nesta ordem:

1. O Hibernate cria as tabelas a partir das entidades (`ddl-auto=create-drop`).
2. O `src/main/resources/h2/schema.sql` cria as 4 views e registra as 2 procedures.
3. O `src/main/resources/h2/data.sql` insere os mesmos dados de teste do `TorneioTCG_SQL.sql`.
4. O `AdminInitializer` cria o administrador.

Contas prontas para login:

| Email | Senha | Tipo | `contaId` |
|---|---|---|---|
| `admin@tcg.com` | `admin123` | ADMIN | 6 |
| `contato@cardhouse.com.br` | `123456` | LOJA (Card House) | 1 |
| `eric@email.com` | `123456` | JOGADOR | 2 |
| `samuel@email.com` | `123456` | JOGADOR | 3 |
| `vinicius@email.com` | `123456` | JOGADOR | 4 |
| `lucas@email.com` | `123456` | JOGADOR | 5 |

Também vêm carregados:
- os 5 jogos com seus formatos;
- a loja Card House com endereço;
- o evento "Dia da Troca";
- o torneio **1 "Standard Semanal"**, com 4 vagas e 4 inscrições já confirmadas, pronto para sortear a chave.

> Os dados ficam só na memória: **tudo volta ao estado inicial quando a aplicação reinicia**.

### 3. Teste rápido com o torneio de exemplo

```bash
API=http://localhost:8080
JSON="Content-Type: application/json"

# Login da loja Card House (copie o token da resposta)
curl -X POST $API/auth/login -H "$JSON" -d '{"email":"contato@cardhouse.com.br","senha":"123456"}'
LOJA="Authorization: Bearer <token>"

# Encerra as inscrições e sorteia a chave (2 semifinais + final)
curl -X PUT  $API/torneios/1/status -H "$LOJA" -H "$JSON" -d '{"status":"INSCRICOES_ENCERRADAS"}'
curl -X POST $API/torneios/1/chaveamento -H "$LOJA"

# Resultado da semifinal 1; o vencedor vai sozinho para a final
curl -X POST $API/partidas/1/resultado -H "$LOJA" -H "$JSON" -d '{"gamesA":2,"gamesB":0,"resultado":"VITORIA_A"}'
curl $API/torneios/1/chaveamento
```

### 4. Consultando o banco pelo navegador (H2 Console)

1. Com a aplicação rodando, abra **http://localhost:8080/h2-console**.
2. Preencha a tela de login exatamente assim:

   | Campo | Valor |
   |---|---|
   | Driver Class | `org.h2.Driver` |
   | JDBC URL | `jdbc:h2:mem:tcg_torneios` |
   | User Name | `sa` |
   | Password | *(deixe vazio)* |

   > O console sugere `jdbc:h2:~/test` por padrão. Troque pela URL acima, senão ele abre outro banco, vazio.

3. Clique em **Connect** e rode consultas normalmente:

   ```sql
   SELECT * FROM conta;
   SELECT * FROM vw_torneio_vagas;
   SELECT * FROM vw_chaveamento WHERE torneio_id = 1 ORDER BY rodada, mesa;
   ```

### 5. Diferenças entre H2 e MySQL

A API responde igual nos dois bancos (o mesmo teste de 214 verificações passa em ambos), mas por baixo há diferenças:

| Item | MySQL | H2 |
|---|---|---|
| Tabelas | Criadas pelo script SQL | Criadas pelo Hibernate a partir das entidades |
| Triggers | Executadas pelo banco | Não existem. As mesmas regras (bloquear torneio finalizado, lista de espera, `cancelado_em`, `finalizado_em`) já são aplicadas pelos services. |
| Procedures | Escritas em SQL | Reescritas em Java em `config/h2/H2Procedures.java` e registradas com os mesmos nomes; os services chamam `CALL sp_...` do mesmo jeito |
| Views | Do script | Recriadas em `h2/schema.sql` |
| `CHECK` e `UNIQUE` do script | No banco | Não existem nas tabelas; a API valida antes de gravar |
| `ON DELETE CASCADE / SET NULL` | Do script | Replicados nas entidades com `@OnDelete` |
| Dados | Permanentes | Somem ao reiniciar |
| Senhas das contas de exemplo | Hash fictício (sem login) | `123456` |
| Tamanho da chave | Até 64 vagas (limite da procedure) | Até 256 vagas |

Por isso, o comportamento final deve ser conferido no MySQL: o H2 não testa as triggers nem as procedures originais.

### 6. Voltando para o MySQL / removendo o H2

- **Para usar o MySQL sem mexer em nada:** suba com o perfil `mysql` ([Com MySQL](#com-mysql)).
- **Para remover o H2 quando não for mais necessário:**
  1. Em `src/main/resources/application.properties`, troque `spring.profiles.default=h2` por `spring.profiles.default=mysql`.
  2. Apague `application-h2.properties`, a pasta `src/main/resources/h2/` e o arquivo `config/h2/H2Procedures.java`.
  3. No `pom.xml`, remova as dependências `com.h2database:h2` e `spring-boot-h2console`.
  4. No `SecurityConfig`, remova a linha que libera `/h2-console/**`.

  Os `@OnDelete` das entidades podem ficar: eles só documentam o que o script SQL já faz. Depois de remover o H2, o teste `contextLoads` volta a precisar do MySQL rodando.

---

## Problemas comuns ao rodar

Quando a aplicação não sobe, o Spring imprime dezenas de linhas de erro em cascata. A causa real costuma estar no **último `Caused by:`** do log.

| Mensagem no log | Causa | Solução |
|---|---|---|
| `Communications link failure`, `Connection refused` ou `Unable to determine Dialect without JDBC metadata` | Perfil `mysql` ativo, mas não há MySQL rodando em `localhost:3306` | Inicie o MySQL ou suba sem perfil, usando o H2 |
| `Unknown database 'tcg_torneios'` | O MySQL está rodando, mas o script não foi executado | Rode o `TorneioTCG_SQL.sql` |
| `Access denied for user 'root'@'localhost'` | A senha do seu MySQL não é `root` | Defina `DB_USERNAME` / `DB_PASSWORD` |
| `Port 8080 was already in use` | Outro programa usando a porta 8080 | Encerre o outro programa ou suba com `--server.port=8081` |
| Login devolve 401 com `eric@email.com` | No MySQL, as contas do script têm hash fictício | Use o admin ou cadastre uma conta. No H2, a senha é `123456`. |

---

## O que foi feito nesta entrega

### Ponto de partida

O projeto tinha a estrutura de pacotes com classes vazias. As únicas classes com conteúdo, `UsuarioJogador` e `UsuarioLoja`, usavam `UUID` e as tabelas `usuariojogador`/`usuarioloja`, que **não existem** no `TorneioTCG_SQL.sql`.

### O que foi implementado

| # | Camada | O que foi feito |
|---|---|---|
| 1 | **Entidades** (`entity/`) | 17 entidades JPA, uma para cada tabela do script, mapeando colunas, tamanhos e relacionamentos. `conta` guarda o login; `jogador`, `loja` e `administrador` usam `conta_id` como chave primária (`@MapsId`), igual ao script. |
| 2 | **Enums** (`entity/enums/`) | 14 enums Java, um para cada coluna `ENUM` do SQL (`StatusTorneio`, `StatusInscricao`, `ResultadoPartida`...). |
| 3 | **Repositórios** (`repository/`) | 17 repositórios Spring Data com consultas derivadas e filtros opcionais (`?lojaId=&status=`...). |
| 4 | **DTOs** (`dto/`) | `record`s de entrada com Bean Validation (`@NotBlank`, `@Email`, `@Size`...) e `record`s de saída para as 4 views. |
| 5 | **Services** (`service/`) | 19 services com as regras de negócio, mais o `PermissaoService`, que centraliza quem pode fazer o quê. |
| 6 | **Controllers** (`controller/`) | 18 controllers REST com CRUD completo e as ações especiais (check-in, chaveamento, resultado...). |
| 7 | **Segurança** (`security/`) | JWT stateless: `JwtUtils`, `JwtFilter`, `CustomUserDetailsService` e `SecurityConfig` (com CORS liberado para o front). |
| 8 | **Banco** | Endpoints que chamam as procedures `sp_gerar_chaveamento` e `sp_registrar_resultado` e leem as views `vw_trofeus`, `vw_agenda_loja`, `vw_chaveamento` e `vw_torneio_vagas`. |
| 9 | **Erros** (`exception/`) | `GlobalExceptionHandler`: respostas no padrão ProblemDetail e mensagens das triggers (`SIGNAL SQLSTATE '45000'`) devolvidas como **409** em vez de 500. |
| 10 | **Inicialização** (`config/`) | `AdminInitializer` (cria o admin) e `GameInitializer` (cadastra jogos e formatos se o banco estiver vazio, com os mesmos dados do script). |
| 11 | **Configuração** | `application.properties` com MySQL, JWT e admin, tudo sobrescrevível por variáveis de ambiente. |
| 12 | **Utilitário** (`util/`) | `SlugUtils` para gerar slugs (`"Pokémon TCG!"` → `pokemon-tcg`). |

### Tabela do SQL → código

| Tabela | Entidade | Service | Rota |
|---|---|---|---|
| `conta` | `Conta` | `ContaService` | `/contas` |
| `endereco` | `Endereco` | `EnderecoService` | `/enderecos` |
| `loja` | `UsuarioLoja` | `UsuarioLojaService` | `/lojas` |
| `jogador` | `UsuarioJogador` | `UsuarioJogadorService` | `/jogadores` |
| `administrador` | `Administrador` | `AdministradorService` | `/administradores` |
| `loja_membro` | `LojaMembro` | `LojaMembroService` | `/loja-membros` |
| `jogo` | `Jogo` | `JogoService` | `/jogos` |
| `formato` | `Formato` | `FormatoService` | `/formatos` |
| `evento` | `Evento` | `EventoService` | `/eventos` |
| `evento_participacao` | `EventoParticipacao` | `EventoParticipacaoService` | `/evento-participacoes` |
| `torneio` | `Torneio` | `TorneioService` | `/torneios` |
| `inscricao` | `Inscricao` | `InscricaoService` | `/inscricoes` |
| `rodada` | `Rodada` | `RodadaService` | `/rodadas` |
| `partida` | `Partida` | `PartidaService` | `/partidas` |
| `game` | `Game` | `GameService` | `/games` |
| `torneio_resultado` | `TorneioResultado` | `TorneioResultadoService` | `/torneio-resultados` |
| `notificacao` | `Notificacao` | `NotificacaoService` | `/notificacoes` |

Login e cadastro ficam em `AuthService` / `AuthController` (`/auth`).

### Arquivos removidos ou renomeados

| Antes | Depois | Motivo |
|---|---|---|
| `entity/Jogador`, `dto/Jogador`, `JogadorRepository`, `JogadorService`, `JogadorController` (vazios) | removidos | Duplicavam `UsuarioJogador`, que agora mapeia a tabela `jogador`. |
| `dto/Formato`, `dto/Jogo`, `dto/Partida`, `dto/Torneio`, `dto/UsuarioJogador`, `dto/UsuarioLoja` (vazios) | `FormatoRequest`, `JogoRequest`, `PartidaRequest`, `TorneioRequest`, `UsuarioJogadorRequest`, `UsuarioLojaRequest`... | Tinham o mesmo nome das entidades, o que obrigaria a usar nomes completos de pacote nos services. |
| `controller/jogoController.java` | `controller/JogoController.java` | Convenção Java (classe com inicial maiúscula). A renomeação já está registrada no git com `git mv`. |
| `entity/UsuarioJogador`, `entity/UsuarioLoja` (UUID, tabelas `usuariojogador`/`usuarioloja`) | mesmas classes, agora nas tabelas `jogador` e `loja` com `conta_id` | Ficar igual ao script SQL. |

Não foram alterados (não existem no script SQL): `Assinatura`, `noticia`, `AssinaturaRepository`, `NoticiaRepository`, `AssinaturaService`, `NoticiaService` e `AssinatruraController`.

### Diferenças em relação ao projeto de referência (dungeonFinderBackend)

A organização (entity / repository / service / controller / dto / security / config) e os padrões (Lombok, `ResponseStatusException`, `ResponseEntity`, `record` como DTO, JWT) seguem o dungeonFinder. Os ajustes foram:

- **IDs numéricos** (`Long`/`Integer`, `AUTO_INCREMENT`) em vez de `UUID`, porque é o que o script define.
- **O usuário logado vem do token JWT**, e não do header `X-Usuario-Id`, que qualquer cliente poderia falsificar.
- **O `JwtFilter` não quebra com token inválido ou expirado**: a requisição segue como anônima e as rotas protegidas respondem 401 (no dungeonFinder isso virava erro 500).
- **Respostas de erro padronizadas** (ProblemDetail) e mensagens das triggers repassadas ao cliente.
- **Validação dos DTOs** com Bean Validation (`@Valid`).
- **`ddl-auto=none`** no MySQL: o schema pertence ao script SQL.

### Atualização: H2 para testes

Rodar sem um MySQL local gerava centenas de linhas de erro (`Connection refused` em cascata). Para permitir testes sem instalar banco, foi adicionado o H2 como perfil temporário:

| Arquivo | Mudança |
|---|---|
| `pom.xml` | Dependências `com.h2database:h2` e `spring-boot-h2console` (console web do H2 no Spring Boot 4) |
| `application.properties` | Ficou só com o que é comum aos dois bancos, com `spring.profiles.default=h2` |
| `application-mysql.properties` (novo) | Configuração do MySQL que antes ficava no `application.properties` |
| `application-h2.properties` (novo) | H2 em memória no modo MySQL, tabelas criadas pelo Hibernate e console em `/h2-console` |
| `h2/schema.sql` (novo) | As 4 views do script e o registro das procedures |
| `h2/data.sql` (novo) | Os dados de teste do script, com senha real `123456` |
| `config/h2/H2Procedures.java` (novo) | `sp_gerar_chaveamento` e `sp_registrar_resultado` reescritas em Java (o H2 não roda procedures em SQL) |
| 6 entidades | `@OnDelete` para replicar os `ON DELETE CASCADE / SET NULL` do script (não muda nada no MySQL) |
| `SecurityConfig` | Libera `/h2-console/**` e permite frames da mesma origem (o console usa frames) |
| `GlobalExceptionHandler` | Procura o SQLSTATE `45000` em toda a cadeia de erros, porque o H2 pode embrulhar o erro da procedure |

---

## Estrutura do projeto

```
src/main/java/senac/com/backendTCG
├── BackendTcgApplication.java
├── config/          AdminInitializer, GameInitializer
│   └── h2/          H2Procedures (procedures em Java, só no perfil h2)
├── controller/      18 controllers REST
├── dto/             records de request/response
├── entity/          17 entidades JPA
│   └── enums/       14 enums (colunas ENUM do SQL)
├── exception/       GlobalExceptionHandler
├── repository/      17 repositórios Spring Data
├── security/        JwtUtils, JwtFilter, CustomUserDetailsService, SecurityConfig
├── service/         regras de negócio + PermissaoService
└── util/            SlugUtils

src/main/resources
├── application.properties         configurações comuns + perfil padrão
├── application-h2.properties      perfil h2 (testes, temporário)
├── application-mysql.properties   perfil mysql
└── h2/
    ├── schema.sql                 views e procedures do H2
    └── data.sql                   dados de teste do H2
```

---

## Autenticação

1. Cadastre-se em `POST /auth/registro/jogador` ou `POST /auth/registro/loja`, ou faça login em `POST /auth/login`.
2. A resposta traz o token:

   ```json
   { "token": "eyJhbGciOi...", "contaId": 8, "email": "loja@exemplo.com", "tipo": "LOJA" }
   ```

3. Envie o token nas rotas protegidas:

   ```
   Authorization: Bearer eyJhbGciOi...
   ```

O token vale 24 h. Contas desativadas (`ativo = false`) ou excluídas (`deletado_em` preenchido) não fazem login e têm os tokens já emitidos recusados.

---

## Permissões

As consultas (`GET`) de vitrine são públicas. Para escrever, é preciso token, e cada service confere o perfil:

| Perfil | Quem é |
|---|---|
| **Público** | Qualquer um, sem token |
| **Logado** | Qualquer token válido |
| **Admin** | Conta do tipo `ADMIN` |
| **Dono** | A própria conta (o id do token é o id do recurso) |
| **Equipe da loja** | A conta da loja, qualquer membro ativo em `loja_membro` (PROPRIETARIO, ORGANIZADOR ou JUIZ) ou um Admin |
| **Proprietário** | A conta da loja, um membro ativo com papel PROPRIETARIO ou um Admin |

Ao se cadastrar, a loja entra automaticamente como `PROPRIETARIO` na própria equipe, como nos dados de exemplo do script. Um jogador adicionado à equipe (por exemplo, como ORGANIZADOR) passa a poder gerenciar os torneios e eventos daquela loja.

---

## Endpoints

Datas no formato ISO-8601 (`2026-12-20T19:00:00`, ou `2000-05-31` para data de nascimento). Enums em maiúsculas, iguais ao SQL (`INSCRICOES_ABERTAS`, `VITORIA_A`...). Todas as respostas são JSON.

### Autenticação — `/auth`

| Método | Rota | Acesso | Descrição |
|---|---|---|---|
| POST | `/auth/registro/jogador` | Público | Cria a conta JOGADOR e o perfil. Devolve o token (201). |
| POST | `/auth/registro/loja` | Público | Cria a conta LOJA, o perfil e o vínculo PROPRIETARIO. Devolve o token (201). |
| POST | `/auth/login` | Público | `{ email, senha }` → token |
| GET | `/auth/me` | Logado | Dados da conta do token |

Corpo do registro de jogador: `{ email, senha, nome, nickname, imagemPerfil?, bio?, dataNascimento?, cidade?, estado? }`
Corpo do registro de loja: `{ email, senha, nome, slug?, descricao?, imagemPerfil?, imagemBanner?, telefone? }` (sem slug, ele é gerado a partir do nome)

### Contas — `/contas`

| Método | Rota | Acesso | Descrição |
|---|---|---|---|
| GET | `/contas?tipo=` | Admin | Lista as contas (filtro opcional por `LOJA`, `JOGADOR` ou `ADMIN`) |
| GET | `/contas/{id}` | Dono ou Admin | Busca uma conta |
| PUT | `/contas/{id}/senha` | Dono | `{ senhaAtual, novaSenha }` |
| PUT | `/contas/{id}/status` | Admin | `{ ativo }`: ativa ou desativa o login |
| DELETE | `/contas/{id}` | Dono ou Admin | Soft delete (preenche `deletado_em`) |

### Administradores — `/administradores` (somente Admin)

| Método | Rota | Descrição |
|---|---|---|
| GET | `/administradores` | Lista |
| GET | `/administradores/{id}` | Busca |
| POST | `/administradores` | `{ email, senha, nome }`: cria conta ADMIN e perfil |
| PUT | `/administradores/{id}` | `{ nome }` |
| DELETE | `/administradores/{id}` | Soft delete. Um admin não pode excluir a si mesmo. |

### Jogadores — `/jogadores`

| Método | Rota | Acesso | Descrição |
|---|---|---|---|
| GET | `/jogadores` | Público | Lista (sem os excluídos) |
| GET | `/jogadores/{id}` | Público | Busca |
| GET | `/jogadores/nickname/{nickname}` | Público | Busca por nickname |
| GET | `/jogadores/{id}/trofeus` | Público | Ouro, prata, bronze e torneios disputados (view `vw_trofeus`) |
| POST | `/jogadores` | Público | Mesmo corpo de `/auth/registro/jogador`, mas devolve o perfil criado |
| PUT | `/jogadores/{id}` | Dono ou Admin | `{ nome, nickname, imagemPerfil?, bio?, dataNascimento?, cidade?, estado? }` |
| DELETE | `/jogadores/{id}` | Dono ou Admin | Soft delete da conta |

### Lojas — `/lojas`

| Método | Rota | Acesso | Descrição |
|---|---|---|---|
| GET | `/lojas` | Público | Lista |
| GET | `/lojas/{id}` | Público | Busca |
| GET | `/lojas/slug/{slug}` | Público | Busca pelo slug |
| GET | `/lojas/{id}/agenda` | Público | Eventos e torneios da loja (view `vw_agenda_loja`) |
| POST | `/lojas` | Público | Mesmo corpo de `/auth/registro/loja`, mas devolve a loja criada |
| PUT | `/lojas/{id}` | Proprietário | `{ nome, slug?, descricao?, imagemPerfil?, imagemBanner?, telefone?, enderecoId? }` |
| PUT | `/lojas/{id}/verificacao` | Admin | `{ verificada }`: selo de loja verificada |
| DELETE | `/lojas/{id}` | Dono ou Admin | Soft delete da conta |

### Equipe da loja — `/loja-membros`

| Método | Rota | Acesso | Descrição |
|---|---|---|---|
| GET | `/loja-membros?lojaId=` | Equipe da loja (sem `lojaId`: Admin) | Lista a equipe |
| GET | `/loja-membros/{id}` | Equipe da loja | Busca |
| POST | `/loja-membros` | Proprietário | `{ lojaId, contaId, papel, ativo? }`, com papel `PROPRIETARIO`, `ORGANIZADOR` ou `JUIZ` |
| PUT | `/loja-membros/{id}` | Proprietário | `{ papel, ativo? }` |
| DELETE | `/loja-membros/{id}` | Proprietário | Remove da equipe |

### Endereços — `/enderecos`

| Método | Rota | Acesso | Descrição |
|---|---|---|---|
| GET | `/enderecos`, `/enderecos/{id}` | Público | Lista / busca |
| POST | `/enderecos` | Loja, membro de equipe ou Admin | `{ cep?, logradouro, numero?, complemento?, bairro?, cidade, estado, latitude?, longitude?, referencia? }` (CEP com 8 dígitos, sem traço) |
| PUT | `/enderecos/{id}` | Loja, membro de equipe ou Admin | Mesmo corpo |
| DELETE | `/enderecos/{id}` | Loja, membro de equipe ou Admin | Lojas, torneios e eventos que usavam o endereço ficam sem endereço (`ON DELETE SET NULL`) |

Para colocar um endereço numa loja: crie-o em `POST /enderecos` e envie o `enderecoId` em `PUT /lojas/{id}`.

### Jogos — `/jogos` e Formatos — `/formatos`

| Método | Rota | Acesso | Descrição |
|---|---|---|---|
| GET | `/jogos?ativo=` | Público | Lista (filtro opcional) |
| GET | `/jogos/{id}` | Público | Busca |
| POST | `/jogos` | Admin | `{ nome, slug?, icone?, ativo? }` |
| PUT | `/jogos/{id}` | Admin | Mesmo corpo |
| DELETE | `/jogos/{id}` | Admin | Bloqueado (409) se o jogo tiver torneios. Nesse caso, desative com `ativo: false`. |
| GET | `/formatos?jogoId=` | Público | Lista (filtro opcional por jogo) |
| GET | `/formatos/{id}` | Público | Busca |
| POST | `/formatos` | Admin | `{ jogoId, nome, ativo? }` |
| PUT | `/formatos/{id}` | Admin | Mesmo corpo |
| DELETE | `/formatos/{id}` | Admin | Bloqueado (409) se o formato tiver torneios |

### Eventos — `/eventos`

| Método | Rota | Acesso | Descrição |
|---|---|---|---|
| GET | `/eventos?lojaId=&status=` | Público | Lista (sem os excluídos) |
| GET | `/eventos/{id}` | Público | Busca |
| POST | `/eventos` | Equipe da loja | `{ lojaId, titulo, descricao?, imagem?, tipo?, vagasMax?, enderecoId?, dataInicio, dataFim?, status? }` |
| PUT | `/eventos/{id}` | Equipe da loja | Mesmo corpo (o `lojaId` é ignorado). Notifica os participantes. |
| DELETE | `/eventos/{id}` | Equipe da loja | Soft delete |

### Participações em eventos — `/evento-participacoes`

| Método | Rota | Acesso | Descrição |
|---|---|---|---|
| GET | `/evento-participacoes?eventoId=&jogadorId=` | Público | Lista |
| GET | `/evento-participacoes/{id}` | Público | Busca |
| POST | `/evento-participacoes` | Jogador ou equipe da loja | `{ eventoId }` para o próprio jogador, ou `{ eventoId, jogadorId }` quando a equipe inscreve alguém |
| PUT | `/evento-participacoes/{id}` | Jogador dono ou equipe | `{ status }` (`CONFIRMADO` ou `CANCELADO`) |
| DELETE | `/evento-participacoes/{id}` | Jogador dono ou equipe | Remove |

### Torneios — `/torneios`

| Método | Rota | Acesso | Descrição |
|---|---|---|---|
| GET | `/torneios?lojaId=&jogoId=&status=` | Público | Lista (sem os excluídos) |
| GET | `/torneios/{id}` | Público | Busca |
| POST | `/torneios` | Equipe da loja | `{ lojaId, jogoId, formatoId?, titulo, descricao?, imagem?, vagasMax, taxaInscricao?, premiacao?, enderecoId?, inscricoesAte?, dataInicio }`. Nasce como `RASCUNHO`. |
| PUT | `/torneios/{id}` | Equipe da loja | Mesmo corpo. Bloqueado se o torneio estiver FINALIZADO. |
| PUT | `/torneios/{id}/status` | Equipe da loja | `{ status }`. Notifica os inscritos ao iniciar, finalizar ou cancelar. |
| DELETE | `/torneios/{id}` | Equipe da loja | Soft delete. Bloqueado se o torneio estiver FINALIZADO. |
| POST | `/torneios/{id}/chaveamento` | Equipe da loja | Sorteia a chave (procedure `sp_gerar_chaveamento`) |
| GET | `/torneios/{id}/chaveamento` | Público | Chave pronta para exibir (view `vw_chaveamento`) |
| GET | `/torneios/{id}/vagas` | Público | Inscritos, vagas restantes, lista de espera e pagamentos pendentes (view `vw_torneio_vagas`) |

Status do torneio: `RASCUNHO` → `INSCRICOES_ABERTAS` → `INSCRICOES_ENCERRADAS` → `EM_ANDAMENTO` → `FINALIZADO` (ou `CANCELADO`).

### Inscrições — `/inscricoes`

| Método | Rota | Acesso | Descrição |
|---|---|---|---|
| GET | `/inscricoes?torneioId=&jogadorId=&status=` | Público | Lista |
| GET | `/inscricoes/{id}` | Público | Busca |
| POST | `/inscricoes` | Jogador ou equipe da loja | `{ torneioId }` para o próprio jogador, ou `{ torneioId, jogadorId }` quando a equipe inscreve alguém |
| PUT | `/inscricoes/{id}` | Equipe da loja | `{ status?, pagamentoStatus?, seed? }` |
| PUT | `/inscricoes/{id}/check-in` | Equipe da loja | Confirma presença (`CONFIRMADO`). Só os confirmados entram na chave. |
| PUT | `/inscricoes/{id}/cancelar` | Jogador dono ou equipe | Desinscrição. Libera a vaga para a lista de espera. |
| DELETE | `/inscricoes/{id}` | Equipe da loja | Exclusão definitiva. Bloqueada (409) se a inscrição já estiver em alguma partida. |

### Rodadas — `/rodadas`

Normalmente são criadas pelo chaveamento. O CRUD serve para ajustes manuais.

| Método | Rota | Acesso | Descrição |
|---|---|---|---|
| GET | `/rodadas?torneioId=` | Público | Lista |
| GET | `/rodadas/{id}` | Público | Busca |
| POST | `/rodadas` | Equipe da loja | `{ torneioId, numero, nome, status? }` |
| PUT | `/rodadas/{id}` | Equipe da loja | `{ numero, nome, status? }`. Ao iniciar, preenche `iniciada_em` e notifica os inscritos. |
| DELETE | `/rodadas/{id}` | Equipe da loja | Apaga a rodada e as partidas dela |

### Partidas — `/partidas`

| Método | Rota | Acesso | Descrição |
|---|---|---|---|
| GET | `/partidas?rodadaId=&torneioId=` | Público | Lista |
| GET | `/partidas/{id}` | Público | Busca |
| POST | `/partidas` | Equipe da loja | `{ rodadaId, mesa, inscricaoAId?, inscricaoBId?, proximaPartidaId?, proximoSlot?, status?, resultado?, gamesA?, gamesB?, gamesEmpate?, observacao? }`. O vencedor é calculado a partir do resultado. |
| PUT | `/partidas/{id}` | Equipe da loja | Mesmo corpo (edição manual) |
| POST | `/partidas/{id}/resultado` | Equipe da loja | `{ gamesA, gamesB, gamesEmpate?, resultado }`: registra o resultado e avança o vencedor na chave (procedure `sp_registrar_resultado`) |
| DELETE | `/partidas/{id}` | Equipe da loja | Apaga a partida e os games dela |

Resultados possíveis: `VITORIA_A`, `VITORIA_B`, `EMPATE`, `WO_A`, `WO_B`, `DUPLO_NO_SHOW`.

### Games — `/games`

| Método | Rota | Acesso | Descrição |
|---|---|---|---|
| GET | `/games?partidaId=` | Público | Lista |
| GET | `/games/{id}` | Público | Busca |
| POST | `/games` | Equipe da loja | `{ partidaId, numero (1 a 5), resultado (A, B ou EMPATE), duracaoMin? }` |
| PUT | `/games/{id}` | Equipe da loja | `{ numero, resultado, duracaoMin? }` |
| DELETE | `/games/{id}` | Equipe da loja | Remove |

### Resultados finais — `/torneio-resultados`

| Método | Rota | Acesso | Descrição |
|---|---|---|---|
| GET | `/torneio-resultados?torneioId=&jogadorId=` | Público | Classificação |
| GET | `/torneio-resultados/{id}` | Público | Busca |
| POST | `/torneio-resultados` | Equipe da loja | `{ torneioId, jogadorId, colocacao, vitorias?, derrotas?, empates?, premioRecebido? }`. Pode ser registrado com o torneio já FINALIZADO. |
| PUT | `/torneio-resultados/{id}` | Equipe da loja | `{ colocacao, vitorias?, derrotas?, empates?, premioRecebido? }` |
| DELETE | `/torneio-resultados/{id}` | Equipe da loja | Remove |

### Notificações — `/notificacoes`

Sempre as notificações da conta do token.

| Método | Rota | Acesso | Descrição |
|---|---|---|---|
| GET | `/notificacoes` | Logado | Todas, da mais nova para a mais antiga |
| GET | `/notificacoes/nao-lidas` | Logado | Só as não lidas |
| GET | `/notificacoes/nao-lidas/total` | Logado | Número de não lidas (para o "badge" do front) |
| GET | `/notificacoes/{id}` | Dono ou Admin | Busca |
| POST | `/notificacoes` | Admin | `{ contaId, tipo, titulo, mensagem, torneioId?, eventoId?, partidaId? }`: aviso manual |
| PUT | `/notificacoes/{id}/lida` | Dono | Marca como lida (`lida_em`) |
| PUT | `/notificacoes/lidas` | Logado | Marca todas como lidas |
| DELETE | `/notificacoes/{id}` | Dono | Remove |

---

## Integração com triggers, views e procedures

| Objeto do SQL | Como a API trata |
|---|---|
| `trg_torneio_bloqueia_update` / `trg_torneio_bloqueia_delete` | O service já barra edição e exclusão de torneio FINALIZADO com 409 e mensagem clara, antes de chegar ao banco. |
| `trg_partida_bloqueia_update` | Rodadas, partidas, games e inscrições de torneio FINALIZADO também são barrados no service. |
| `trg_inscricao_controla_vagas` | A API calcula `INSCRITO` ou `LISTA_ESPERA` e, depois do INSERT, relê a linha para devolver o status que a trigger decidiu. |
| `trg_inscricao_cancelamento` | Ao cancelar, a API também preenche `cancelado_em` e limpa o `seed`, para a resposta já vir correta. |
| `trg_torneio_finalizacao` | Ao finalizar, a API também preenche `finalizado_em`. |
| `CHECK`s (vagas em potência de 2, taxa ≥ 0, prazo ≤ início, datas do evento, oponentes diferentes, resultado × vencedor, game de 1 a 5, colocação ≥ 1) | Validados nos DTOs e services, com resposta 400 e mensagem em português. |
| `sp_gerar_chaveamento` | `POST /torneios/{id}/chaveamento` |
| `sp_registrar_resultado` | `POST /partidas/{id}/resultado` |
| `vw_chaveamento` | `GET /torneios/{id}/chaveamento` |
| `vw_torneio_vagas` | `GET /torneios/{id}/vagas` |
| `vw_trofeus` | `GET /jogadores/{id}/trofeus` |
| `vw_agenda_loja` | `GET /lojas/{id}/agenda` |

Se alguma regra ainda for barrada pelo banco (um `SIGNAL SQLSTATE '45000'` de trigger ou procedure), o `GlobalExceptionHandler` devolve **409** com a mensagem original. Por exemplo, gerar a chave com inscrições abertas devolve `"Encerre as inscricoes antes de gerar a chave."`.

---

## Regras de negócio

- **Inscrição:** o próprio jogador só se inscreve com o torneio em `INSCRICOES_ABERTAS` e dentro do prazo `inscricoes_ate`. A equipe da loja pode inscrever jogadores também em `RASCUNHO`, igual à trigger.
- **Pagamento:** taxa zero gera `ISENTO`; caso contrário, `PENDENTE`. A equipe marca `PAGO` ou `REEMBOLSADO` com `PUT /inscricoes/{id}`.
- **Lista de espera:** quando alguém que ocupava vaga cancela, o primeiro da `LISTA_ESPERA` (por ordem de inscrição) sobe para `INSCRITO`.
- **Reinscrição:** uma inscrição `CANCELADA` pode ser reativada (o SQL só permite uma inscrição por jogador em cada torneio).
- **Cancelamento:** não é permitido depois que o torneio começou (`EM_ANDAMENTO` ou `FINALIZADO`).
- **Check-in:** muda para `CONFIRMADO` e preenche `check_in_em`. A procedure só sorteia os confirmados, e o número de confirmados precisa ser igual a `vagas_max`.
- **Participação em evento:** só em eventos `PUBLICADO` ou `EM_ANDAMENTO`, respeitando `vagas_max` (null = sem limite).
- **Soft delete:** `conta`, `torneio` e `evento` recebem `deletado_em` e somem das listagens, mas o histórico continua no banco.
- **Slugs:** gerados a partir do nome quando não são enviados. Na edição, slug vazio mantém o atual.
- **Email:** gravado em minúsculas e único. O SQL não tem `UNIQUE` em `conta.email`, então a API faz essa checagem.
- **Privacidade:** o JSON público de jogadores, lojas e administradores não inclui email nem hash de senha.

### Notificações automáticas

| Quando | Tipo | Quem recebe |
|---|---|---|
| Check-in / inscrição confirmada | `INSCRICAO_CONFIRMADA` | O jogador |
| Chave sorteada | `PAREAMENTO` | Inscritos no torneio |
| Torneio → `EM_ANDAMENTO` | `TORNEIO_INICIADO` | Inscritos |
| Rodada → `EM_ANDAMENTO` | `RODADA_INICIADA` | Inscritos |
| Resultado registrado pela procedure | `RESULTADO_REGISTRADO` | Os dois jogadores da partida |
| Torneio → `FINALIZADO` / `CANCELADO` | `TORNEIO_FINALIZADO` / `TORNEIO_CANCELADO` | Inscritos |
| Evento editado | `EVENTO_ATUALIZADO` | Participantes confirmados |
| `POST /notificacoes` (admin) | Qualquer tipo, como `AVISO_GERAL` | A conta escolhida |

---

## Exemplo: um torneio do início ao fim

```bash
API=http://localhost:8080
JSON="Content-Type: application/json"

# 1) A loja se cadastra e recebe o token
curl -X POST $API/auth/registro/loja -H "$JSON" \
  -d '{"email":"loja@exemplo.com","senha":"segredo1","nome":"Minha Loja"}'
# {"token":"eyJ...","contaId":8,"email":"loja@exemplo.com","tipo":"LOJA"}
LOJA="Authorization: Bearer eyJ..."

# 2) Cria o torneio (nasce em RASCUNHO) e abre as inscrições
curl -X POST $API/torneios -H "$LOJA" -H "$JSON" -d '{
  "lojaId": 8, "jogoId": 1, "formatoId": 1, "titulo": "Standard de Sexta",
  "vagasMax": 4, "taxaInscricao": 10,
  "inscricoesAte": "2026-12-20T18:00:00", "dataInicio": "2026-12-20T19:00:00"}'
curl -X PUT $API/torneios/2/status -H "$LOJA" -H "$JSON" -d '{"status":"INSCRICOES_ABERTAS"}'

# 3) Cada jogador se cadastra (/auth/registro/jogador) e se inscreve
#    O 5º inscrito de um torneio de 4 vagas vai para LISTA_ESPERA
curl -X POST $API/inscricoes -H "Authorization: Bearer <token-do-jogador>" -H "$JSON" \
  -d '{"torneioId":2}'

# 4) No dia, a loja faz o check-in de cada inscrição
curl -X PUT $API/inscricoes/10/check-in -H "$LOJA"

# 5) Encerra as inscrições e sorteia a chave
curl -X PUT  $API/torneios/2/status -H "$LOJA" -H "$JSON" -d '{"status":"INSCRICOES_ENCERRADAS"}'
curl -X POST $API/torneios/2/chaveamento -H "$LOJA"

# 6) Registra os resultados; o vencedor avança sozinho para a próxima partida
curl -X POST $API/partidas/7/resultado -H "$LOJA" -H "$JSON" \
  -d '{"gamesA":2,"gamesB":1,"resultado":"VITORIA_A"}'
curl $API/torneios/2/chaveamento

# 7) Finaliza, registra a classificação e confere os troféus
curl -X PUT  $API/torneios/2/status -H "$LOJA" -H "$JSON" -d '{"status":"FINALIZADO"}'
curl -X POST $API/torneio-resultados -H "$LOJA" -H "$JSON" \
  -d '{"torneioId":2,"jogadorId":9,"colocacao":1,"vitorias":2,"premioRecebido":"Booster box"}'
curl $API/jogadores/9/trofeus
```

Os ids acima são ilustrativos: use os que a API devolver.

---

## Formato de erros

Os erros seguem o padrão ProblemDetail (RFC 9457):

```json
{
  "type": "about:blank",
  "title": "Conflict",
  "status": 409,
  "detail": "Encerre as inscricoes antes de gerar a chave.",
  "instance": "/torneios/2/chaveamento"
}
```

Erros de validação trazem também a lista de campos:

```json
{
  "title": "Bad Request",
  "status": 400,
  "detail": "Dados inválidos",
  "erros": { "email": "deve ser um endereço de e-mail bem formado", "nickname": "não deve estar em branco" }
}
```

| Status | Quando acontece |
|---|---|
| 400 | Dados inválidos ou regra de validação (vagas fora da potência de 2, datas invertidas...) |
| 401 | Sem token, token inválido ou expirado, email ou senha errados |
| 403 | Sem permissão para a ação, ou conta desativada no login |
| 404 | Registro não encontrado (inclui os excluídos com soft delete) |
| 409 | Duplicidade (email, nickname, slug...), conflito de estado (torneio finalizado, sem vagas...), regra barrada por trigger ou procedure, ou registro em uso por outra tabela |

---

## Como foi testado

- **Compilação:** `mvn compile` sem erros e sem avisos de API depreciada (Spring Boot 4.1.1, JDK 21+).
- **Banco real:** o `TorneioTCG_SQL.sql` foi executado **sem nenhuma alteração** num MySQL 8.4.6 limpo, e a API subiu apontando para ele.
- **Teste de ponta a ponta:** um script percorreu a API inteira, com **214 verificações, todas aprovadas**, cobrindo:
  - login, cadastro, token inválido, conta desativada ou excluída;
  - CRUD de todos os recursos, filtros, soft delete e erros de validação;
  - permissões: jogador × loja × equipe × admin, e ex-membro perdendo acesso;
  - fluxo completo de torneio: inscrições → lista de espera e promoção → check-in → `sp_gerar_chaveamento` → `sp_registrar_resultado` (vitória, W.O. marcando NO_SHOW, empate recusado pela procedure) → final → finalização → classificação → troféus;
  - bloqueios de torneio finalizado e mensagens das triggers e procedures devolvidas como 409;
  - as 4 views, as notificações automáticas, eventos com vagas e a equipe da loja;
  - o torneio de exemplo do próprio script (Card House) chaveado pelo admin.
- **H2:** o mesmo teste de ponta a ponta rodou com o perfil `h2` e passou nas **214 verificações**, incluindo as procedures reescritas em Java, as views e o `ON DELETE SET NULL`. Também foram conferidos o login no H2 Console e o `mvn test` (o `contextLoads` agora passa sem MySQL).

---

## Observações e próximos passos

- **Script SQL fora do repositório:** quem clonar o projeto não consegue criar o banco sem pedir o `TorneioTCG_SQL.sql` à equipe. Se o schema mudar, as entidades em `entity/` precisam acompanhar, porque o Hibernate não valida nem altera as tabelas.
- **H2 é temporário:** ele não executa as triggers nem as procedures originais do script, então qualquer mudança nessas regras precisa ser testada no MySQL. Se a procedure do script mudar, o `H2Procedures.java` precisa acompanhar. Para remover o H2, veja [Voltando para o MySQL / removendo o H2](#6-voltando-para-o-mysql--removendo-o-h2).
- **Senhas dos dados de exemplo:** os hashes BCrypt do script são fictícios. Para as contas de exemplo funcionarem no MySQL, gere hashes reais ou use o admin do `AdminInitializer` (o `h2/data.sql` já usa o hash real de `123456`).
- **`conta.email` sem `UNIQUE` no SQL:** a API garante a unicidade, mas vale adicionar `UNIQUE KEY uk_conta_email (email)` no script.
- **Chaves com 128 ou 256 vagas:** o `CHECK` aceita, mas a `sp_gerar_chaveamento` só cria até 32 partidas por rodada (a lista de números vai de 1 a 32). Hoje a chave funciona até **64 vagas**. É preciso ampliar essa lista ou limitar `vagas_max` a 64.
- **Endereços não têm dono no schema:** por isso qualquer loja, membro de equipe ou admin pode editá-los. Uma coluna de dono (ou endereço embutido em loja/torneio/evento) resolveria.
- **`Assinatura` e `noticia`:** as classes continuam vazias porque não existem tabelas para elas no SQL.
- **`.gitignore`:** ainda tem marcadores de conflito de merge (`=======` e `>>>>>>> 25bcd51...`) vindos do commit de merge. Vale limpar.
- **Teste `BackendTcgApplicationTests.contextLoads`:** ele sobe a aplicação inteira com o perfil padrão. Enquanto o padrão for o H2, passa sem nada instalado; se o padrão voltar a ser o MySQL, vai precisar do MySQL rodando e do script aplicado.
- **Produção:** troque `JWT_SECRET`, `ADMIN_SENHA` e as credenciais do banco por variáveis de ambiente, e restrinja o CORS (hoje aceita qualquer origem) ao domínio do front.
