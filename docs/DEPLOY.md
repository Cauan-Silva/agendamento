# Publicando o projeto (gratuito)

O projeto fica em três serviços, todos com plano gratuito:

| Parte | Onde | Por quê |
|---|---|---|
| Banco de dados | **Neon** (neon.com) | PostgreSQL gratuito que não expira |
| API (Java) | **Render** (render.com) | Roda o `Dockerfile` da pasta `backend` |
| Front-end (React) | **Vercel** (vercel.com) | Publica o Vite com um clique |

Faça na ordem abaixo: cada etapa usa um endereço gerado pela anterior.

> Limitação do plano gratuito do Render: a API "dorme" depois de 15 minutos sem uso e leva cerca de 1 minuto para acordar no primeiro acesso. Para um projeto de portfólio isso é aceitável — vale avisar no README.

---

## 1. Banco de dados no Neon

1. Crie uma conta em neon.com (pode entrar com o GitHub).
2. Crie um projeto chamado `agendamento`, região **São Paulo** se disponível (ou a mais próxima).
3. Na tela do projeto, clique em **Connect** e copie a *connection string*. Ela tem este formato:
   ```
   postgresql://USUARIO:SENHA@HOST/BANCO?sslmode=require
   ```
4. Separe as partes; você vai precisar delas no Render:
   - `DB_URL` = `jdbc:postgresql://HOST/BANCO?sslmode=require`
   - `DB_USER` = `USUARIO`
   - `DB_PASSWORD` = `SENHA`

As tabelas são criadas sozinhas pelo Flyway quando a API subir.

## 2. API no Render

1. Crie uma conta em render.com com o GitHub.
2. **New → Web Service** → escolha o repositório `agendamento`.
3. Preencha:
   - **Name:** `agendamento-api`
   - **Root Directory:** `backend`
   - **Runtime / Language:** `Docker`
   - **Instance Type:** `Free`
4. Em **Environment Variables**, adicione:

   | Nome | Valor |
   |---|---|
   | `DB_URL` | o `jdbc:postgresql://...` do passo 1 |
   | `DB_USER` | usuário do Neon |
   | `DB_PASSWORD` | senha do Neon |
   | `JWT_SEGREDO` | um texto aleatório com 40+ caracteres (não reutilize senhas) |
   | `ADMIN_EMAIL` | o e-mail que você vai usar no painel |
   | `ADMIN_SENHA` | uma senha forte para o painel |
   | `CORS_ORIGENS` | deixe `http://localhost:5173` por enquanto — você troca no passo 4 |
   | `NOME_NEGOCIO` | o nome que aparece nos e-mails |

   **E-mail (opcional).** Sem estas variáveis a API funciona normalmente, só não envia e-mails. Com uma conta Gmail (com verificação em duas etapas ativada), crie uma *senha de app* em myaccount.google.com/apppasswords e use:

   | Nome | Valor |
   |---|---|
   | `SPRING_MAIL_HOST` | `smtp.gmail.com` |
   | `SPRING_MAIL_PORT` | `587` |
   | `SPRING_MAIL_USERNAME` | seu e-mail do Gmail |
   | `SPRING_MAIL_PASSWORD` | a senha de app de 16 letras (não a senha normal) |
   | `MAIL_REMETENTE` | o mesmo e-mail do Gmail |

   Serviços próprios de e-mail transacional (Brevo, Resend, Mailgun etc.) também funcionam: use o host, a porta e o usuário SMTP que eles fornecem.

5. Clique em **Create Web Service** e espere o primeiro deploy (alguns minutos, porque compila o Maven).
6. Quando terminar, o Render mostra o endereço, algo como `https://agendamento-api-xxxx.onrender.com`. Teste:
   ```
   https://agendamento-api-xxxx.onrender.com/api/servicos
   ```

## 3. Front-end na Vercel

1. Crie uma conta em vercel.com com o GitHub.
2. **Add New → Project** → importe o repositório `agendamento`.
3. Preencha:
   - **Root Directory:** `frontend`
   - **Framework Preset:** `Vite` (costuma vir automático)
4. Em **Environment Variables**:

   | Nome | Valor |
   |---|---|
   | `VITE_API_URL` | `https://agendamento-api-xxxx.onrender.com/api` |
   | `VITE_NOME_NEGOCIO` | o nome que deve aparecer no topo |

5. Clique em **Deploy**. Ao final, a Vercel mostra o endereço, como `https://agendamento-xxxx.vercel.app`.

## 4. Liberar o front-end na API

No Render, volte em **Environment** e troque `CORS_ORIGENS` para o endereço da Vercel (sem barra no final):
```
https://agendamento-xxxx.vercel.app
```
Na mesma tela, crie `URL_SITE` com esse mesmo endereço: é ele que vai no link "cancelar ou remarcar" dos e-mails. Se quiser mudar o prazo para o cliente mexer no horário pelo link (padrão: até 2 horas antes), crie também `ANTECEDENCIA_HORAS`.

Salve; o Render reinicia a API sozinho. Pronto: abra o endereço da Vercel e faça um agendamento. O painel fica em `https://agendamento-xxxx.vercel.app/#/painel`.

## Depois de publicado

- Cada `git push` na `main` publica as mudanças automaticamente no Render e na Vercel.
- Coloque o link da Vercel no topo do README e no seu currículo/LinkedIn.
- **Logins da equipe:** entre no painel com o administrador, abra **Acessos** e crie um login para cada profissional. Cada um vê só a própria agenda e troca a senha em **Minha conta**.
- **Lembretes na véspera:** rodam todo dia às 18h, mas no plano gratuito do Render a API dorme quando ninguém usa — se ela estiver dormindo nesse horário, o lembrete do dia não sai. Para garantir, use um serviço gratuito de "ping" (como cron-job.org) que acesse `https://agendamento-api-xxxx.onrender.com/api/servicos` a cada 10 minutos entre 17h e 19h, ou passe a API para um plano pago.
- Problemas comuns:
  - **Front-end mostra "Sem conexão"**: a API pode estar acordando (espere 1 minuto e tente de novo) ou o `CORS_ORIGENS` não bate exatamente com o endereço da Vercel.
  - **Deploy da API falha no início**: confira `DB_URL` (precisa começar com `jdbc:`) e se `JWT_SEGREDO` tem pelo menos 32 caracteres.
