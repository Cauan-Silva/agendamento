# Agendamento para pequenos negócios

API para barbearias, salões, clínicas e outros negócios que trabalham com hora marcada. O cliente escolhe o serviço, o profissional e o dia, e a API mostra só os horários realmente livres, impedindo que duas pessoas reservem o mesmo horário.

## Tecnologias

- **Back-end:** Java 21 e Spring Boot 3.5 (Web, Data JPA, Validation)
- **Front-end:** React 19 com Vite
- PostgreSQL com migrações via Flyway
- Docker Compose para o banco
- Swagger/OpenAPI para documentação
- JUnit 5, Mockito e AssertJ nos testes

## Como rodar

Pré-requisitos: JDK 21, Maven e Docker.

```bash
# 1. Subir o banco
docker compose up -d

# 2. Rodar a API
cd backend
mvn spring-boot:run
```

A API sobe em `http://localhost:8080` e a documentação interativa fica em `http://localhost:8080/swagger-ui.html`.

O banco já vem com dados de exemplo (3 serviços e 2 profissionais) para você testar na hora. O arquivo `backend/requests.http` tem requisições prontas para usar com a extensão REST Client do VS Code.

### Front-end

Com a API rodando, em outro terminal:

```bash
cd frontend
npm install
npm run dev
```

Abra `http://localhost:5173`. O cliente escolhe serviço, profissional, dia e horário, informa nome e celular e recebe um comprovante com o número do agendamento. Para mudar o endereço da API ou o nome do negócio, copie `frontend/.env.example` para `frontend/.env` e ajuste.

### Testes

```bash
cd backend
mvn test
```

## Endpoints

| Método | Rota | Descrição |
|---|---|---|
| GET | `/api/servicos` | Lista serviços ativos |
| POST | `/api/servicos` | Cadastra serviço |
| PUT | `/api/servicos/{id}` | Atualiza serviço |
| DELETE | `/api/servicos/{id}` | Desativa serviço |
| GET | `/api/profissionais` | Lista profissionais ativos |
| POST | `/api/profissionais` | Cadastra profissional |
| PUT | `/api/profissionais/{id}` | Atualiza profissional |
| DELETE | `/api/profissionais/{id}` | Desativa profissional |
| GET | `/api/disponibilidade?profissionalId=&servicoId=&data=` | Horários livres no dia |
| POST | `/api/agendamentos` | Cria agendamento |
| GET | `/api/agendamentos?profissionalId=&data=` | Agenda do profissional no dia |
| GET | `/api/agendamentos/{id}` | Detalha agendamento |
| PATCH | `/api/agendamentos/{id}/cancelar` | Cancela |
| PATCH | `/api/agendamentos/{id}/concluir` | Marca como concluído |

Erros seguem o padrão RFC 7807 (`application/problem+json`): 400 para dados inválidos, 404 para recurso inexistente, 409 para horário já ocupado e 422 para regras de negócio (fora do expediente, dia de folga etc.).

## Como funciona a disponibilidade

Para um profissional, um serviço e um dia, a API:

1. Verifica se o profissional trabalha naquele dia da semana.
2. Percorre o expediente em passos de 30 minutos (configurável em `agenda.intervalo-minutos`), considerando a duração do serviço.
3. Remove os horários que já passaram.
4. Remove os horários que se sobrepõem a agendamentos não cancelados.

Ao criar um agendamento, as regras são verificadas de novo no servidor. Para evitar que dois clientes reservem o mesmo horário ao mesmo tempo, a linha do profissional é bloqueada no banco (`SELECT ... FOR UPDATE`) durante a transação: a segunda requisição espera a primeira terminar e então recebe `409 Conflict`.

## Estrutura

```
backend/src/main/java/com/cauan/agendamento/
├── config/       # relógio, CORS
├── controller/   # rotas REST
├── dto/          # objetos de entrada e saída
├── exception/    # exceções e tratamento de erros
├── model/        # entidades JPA
├── repository/   # acesso ao banco
└── service/      # regras de negócio
```

## Próximos passos

- [x] Front-end em React (fluxo do cliente)
- [ ] Login com JWT e painel do dono
- [ ] Bloqueio de horários (almoço, folgas)
- [ ] Confirmação e lembrete por e-mail
- [ ] Deploy
