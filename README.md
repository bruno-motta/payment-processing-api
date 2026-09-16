# Payment Processing API

API de estudo para processamento de pagamentos com Stripe, construída com arquitetura hexagonal (Ports and Adapters). O projeto cobre autenticação JWT, cadastro e gestão do próprio usuário, criação de pagamentos, idempotência, retentativas e reembolso.

## Tecnologias

- Java 21 e Spring Boot 3
- Spring Security 6 e JWT
- Spring Data JPA, Flyway e PostgreSQL
- Stripe Java SDK, usando o ambiente de testes da Stripe
- Docker Compose
- Springdoc/OpenAPI e Actuator

Kafka, Transactional Outbox, webhooks Stripe, AWS e CI/CD são próximas etapas; ainda não estão implementados neste repositório.

## Arquitetura

O domínio não depende de Spring, JPA ou Stripe. A aplicação se comunica com dependências externas por portas (`ports/in` e `ports/out`), implementadas por adaptadores de persistência, segurança e gateway de pagamento.

```text
HTTP Controller -> Port in -> Application Service -> Port out -> Adapter
                                              |-> PostgreSQL
                                              |-> Stripe
```

## Configuração local

Pré-requisitos: Java 21, Maven e Docker (para PostgreSQL).

Crie um arquivo `.env` local, sem versioná-lo, com as variáveis abaixo:

```properties
DATABASE_URL_PAYMENT=jdbc:postgresql://localhost:5432/stripe_payment
DB_USERNAME_PAYMENT=postgres
DB_PASSWORD_PAYMENT=sua_senha

JWT_EXPIRATION_PAYMENT=3600000
JWT_SECRET_PAYMENT=uma_chave_secreta_forte

STRIPE_API_KEY=sk_test_...
```

Use somente uma chave de testes da Stripe (`sk_test_...`) no desenvolvimento. Nunca versione credenciais ou o arquivo `.env`.

Suba o banco e a aplicação:

```powershell
docker compose up -d
.\mvnw.cmd spring-boot:run
```

A API fica disponível em `http://localhost:8080`. A documentação OpenAPI fica em `/swagger-ui/index.html`.

Para executar os testes:

```powershell
.\mvnw.cmd test
```

## Autenticação e usuários

| Método | Rota | Autenticação | Descrição |
| --- | --- | --- | --- |
| POST | `/users/register` | Não | Cria uma conta com `ROLE_USER`. |
| POST | `/api/auth` | Não | Autentica e retorna um JWT. |
| GET | `/api/auth/me` | JWT | Retorna o perfil do usuário autenticado. |
| GET | `/users/{userId}` | JWT | Consulta o próprio perfil. |
| PUT | `/users/{userId}` | JWT | Atualiza nome, e-mail e, opcionalmente, senha do próprio perfil. |
| DELETE | `/users/{userId}` | JWT | Desativa a própria conta sem remover dados do banco. |

As operações de leitura, atualização e desativação verificam se o `userId` da rota pertence ao JWT. Um usuário comum não pode acessar a conta de outro usuário.

E-mails são normalizados para letras minúsculas e sem espaços nas extremidades. E-mail duplicado retorna `409 Conflict`; credenciais inválidas retornam `401 Unauthorized`.

Envie o token desta forma nas rotas protegidas:

```http
Authorization: Bearer <token>
```

## Pagamentos

| Método | Rota | Descrição |
| --- | --- | --- |
| POST | `/api/v1/payments` | Cria e processa um pagamento. Requer `Idempotency-Key`. |
| GET | `/api/v1/payments/{paymentId}` | Busca um pagamento do usuário autenticado. |
| POST | `/api/v1/payments/{paymentId}/retry` | Retenta um pagamento que falhou e ainda está dentro do limite. |
| POST | `/api/v1/payments/{paymentId}/refund` | Solicita reembolso de um pagamento aprovado. |

Todas as rotas de pagamento exigem JWT e conferem a propriedade do pagamento pelo usuário autenticado.

Para criar um pagamento, envie também uma chave UUID no header:

```http
Idempotency-Key: 123e4567-e89b-12d3-a456-426614174000
```

A chave é enviada à Stripe e a resposta é persistida localmente para que uma repetição sequencial da mesma requisição devolva a resposta original.

### Máquina de estados

```text
PENDING -> PROCESSING -> APPROVED -> REFUNDED
                     -> FAILED -> PROCESSING (até 3 tentativas)
```

Depois da terceira falha, o pagamento permanece em `FAILED` e não pode ser retentado novamente.

## Respostas de erro relevantes

| Situação | Status HTTP |
| --- | --- |
| Dados de entrada inválidos | 400 |
| Credenciais inválidas | 401 |
| Usuário não autenticado | 401/403 |
| Usuário inexistente | 404 |
| Acesso a perfil de outro usuário | 403 |
| E-mail já cadastrado | 409 |

Wanderson Bruno. 
