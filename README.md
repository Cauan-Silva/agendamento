# Agendamento para pequenos negócios

Sistema de hora marcada para barbearias, salões, clínicas e outros negócios pequenos. O cliente escolhe o serviço, o profissional e o dia, vê só os horários realmente livres e recebe um comprovante. A equipe entra num painel para ver a agenda do dia e marcar atendimentos como concluídos ou cancelados.

<!-- Depois de publicar, troque pelo seu link: -->
<!-- **Ver online:** https://agendamento-xxxx.vercel.app (a API gratuita pode levar ~1 minuto para acordar no primeiro acesso) -->

![Comprovante de agendamento](docs/comprovante.png)

## O que tem aqui

**Para o cliente**
- Serviço → profissional → dia → horário → nome, celular e e-mail (opcional) → comprovante
- Só aparecem horários realmente livres: fora do almoço, de folgas e de feriados
- Confirmação por e-mail na hora e lembrete na véspera

**Para a equipe** (painel em `#/painel`, com login)
- **Agenda do dia** por profissional, com concluir, cancelar e um botão que abre o WhatsApp do cliente com o lembrete já escrito
- **Folgas e feriados:** bloqueios de dias inteiros ou de algumas horas, para uma pessoa ou para o negócio todo
- **Serviços e equipe:** cadastro, edição e desativação, incluindo dias de trabalho, expediente e intervalo de almoço
- **Relatório mensal:** faturado, previsto, cancelamentos, gráfico de atendimentos por dia e desempenho por profissional e por serviço

**Por baixo**
- **Sem horário duplicado:** a API recalcula a disponibilidade e trava o profissional no banco ao agendar; se duas pessoas tentam o mesmo horário, a segunda recebe `409` e a tela oferece outro
- **Faturamento confiável:** o preço é gravado no agendamento, então mudar o preço de um serviço não altera relatórios passados

## Tecnologias

- **Back-end:** Java 21, Spring Boot 3.5 (Web, Data JPA, Validation, Security), JWT
- **Front-end:** React 19 com Vite
- **Banco:** PostgreSQL, com migrações via Flyway
- **Qualidade:** JUnit 5, Mockito e AssertJ; documentação com Swagger/OpenAPI
- **Deploy:** Docker, Render (API), Neon (banco) e Vercel (front-end)

## Como rodar no seu computador

Pré-requisitos: JDK 21, Maven, Node.js e PostgreSQL (instalado ou via Docker).

### 1. Banco de dados

**Com PostgreSQL instalado** (porta 5432), crie o usuário e o banco uma vez:
```sql
CREATE USER agendamento WITH PASSWORD 'agendamento';
CREATE DATABASE agendamento OWNER agendamento;
```

**Ou com Docker** (porta 5433):
```bash
docker compose up -d
```

### 2. API

```bash
cd backend
mvn spring-boot:run                                  # PostgreSQL instalado
mvn spring-boot:run -Dspring-boot.run.profiles=docker  # banco do Docker
```

A API sobe em `http://localhost:8080`; a documentação interativa fica em `http://localhost:8080/swagger-ui.html`. As tabelas e alguns dados de exemplo (3 serviços e 2 profissionais) são criados automaticamente.

### 3. Front-end

Em outro terminal:
```bash
cd frontend
npm install
npm run dev
```

Abra `http://localhost:5173`. O painel fica em `http://localhost:5173/#/painel`, com o login padrão de desenvolvimento **admin@agendamento.local / admin123** (troque com `ADMIN_EMAIL` e `ADMIN_SENHA`).

### Testes

```bash
cd backend
mvn test
```

## Endpoints

| Método | Rota | Acesso | Descrição |
|---|---|---|---|
| POST | `/api/auth/login` | público | Devolve o token JWT do painel |
| GET | `/api/servicos` | público | Lista serviços ativos |
| GET | `/api/profissionais` | público | Lista profissionais ativos |
| GET | `/api/disponibilidade?profissionalId=&servicoId=&data=` | público | Horários livres no dia |
| POST | `/api/agendamentos` | público | Cria agendamento |
| GET | `/api/agendamentos?data=&profissionalId=` | equipe | Agenda do dia (profissional opcional) |
| GET | `/api/agendamentos/{id}` | equipe | Detalha agendamento |
| PATCH | `/api/agendamentos/{id}/cancelar` | equipe | Cancela |
| PATCH | `/api/agendamentos/{id}/concluir` | equipe | Marca como concluído |
| POST/PUT/DELETE | `/api/servicos`, `/api/profissionais` | equipe | Cadastro (DELETE desativa) |
| GET/POST | `/api/bloqueios` | equipe | Lista bloqueios vigentes / cria folga ou feriado |
| DELETE | `/api/bloqueios/{id}` | equipe | Remove bloqueio |
| GET | `/api/relatorios/mensal?ano=&mes=` | equipe | Relatório do mês |

Rotas da equipe exigem `Authorization: Bearer <token>`. Erros seguem o padrão RFC 7807 (`application/problem+json`): 400 dados inválidos, 401 sem login, 404 não encontrado, 409 horário ocupado, 422 regra de negócio (fora do expediente, dia de folga etc.).

## Como funciona a disponibilidade

Para um profissional, um serviço e um dia, a API:

1. Verifica se o profissional trabalha naquele dia da semana.
2. Percorre o expediente em passos de 30 minutos (configurável em `agenda.intervalo-minutos`), considerando a duração do serviço.
3. Remove os horários que já passaram.
4. Remove os horários que caem no almoço ou em um bloqueio (folga da pessoa ou feriado do negócio).
5. Remove os horários que se sobrepõem a agendamentos não cancelados.

Ao criar um agendamento, tudo é verificado de novo no servidor, e a linha do profissional é bloqueada no banco (`SELECT ... FOR UPDATE`) durante a transação: a segunda requisição simultânea espera a primeira terminar e então recebe `409 Conflict`.

## Estrutura

```
backend/src/main/java/com/cauan/agendamento/
├── config/       # relógio, CORS, Swagger
├── controller/   # rotas REST
├── dto/          # objetos de entrada e saída
├── exception/    # exceções e tratamento de erros
├── model/        # entidades JPA
├── notificacao/  # e-mail de confirmação e lembrete diário
├── repository/   # acesso ao banco
├── security/     # login, JWT, regras de acesso
└── service/      # regras de negócio

frontend/src/
├── components/   # etapas do agendamento do cliente
├── painel/       # login, agenda, bloqueios, cadastros e relatório
├── services/     # chamadas à API e sessão
└── utils/        # datas, preços, telefone
```

## Publicar

O passo a passo para colocar tudo online de graça está em [docs/DEPLOY.md](docs/DEPLOY.md).

### E-mail (opcional)

Sem configuração, os e-mails aparecem apenas no log da API. Para enviar de verdade, defina as variáveis `SPRING_MAIL_HOST`, `SPRING_MAIL_PORT`, `SPRING_MAIL_USERNAME`, `SPRING_MAIL_PASSWORD` e `MAIL_REMETENTE` (detalhes em [docs/DEPLOY.md](docs/DEPLOY.md)). O lembrete roda todo dia às 18h para os agendamentos do dia seguinte.

## Ideias para evoluir

- Envio automático pelo WhatsApp com a API oficial (WhatsApp Business)
- Cliente cancelar o próprio horário por um link no e-mail
- Alterar senha e criar logins para cada pessoa da equipe
- Exportar o relatório em planilha
