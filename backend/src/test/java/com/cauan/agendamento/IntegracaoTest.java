package com.cauan.agendamento;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Sobe a aplicação inteira contra um PostgreSQL de verdade: confere as migrações
 * do Flyway, o mapeamento JPA, a segurança e os fluxos principais pela API.
 *
 * Roda na CI (que cria o banco). Na sua máquina, rode com DB_URL definido.
 */
@SpringBootTest
@AutoConfigureMockMvc
@EnabledIfEnvironmentVariable(named = "DB_URL", matches = ".+")
class IntegracaoTest {

    @Autowired
    private MockMvc mvc;
    @Autowired
    private ObjectMapper json;

    /** Uma segunda-feira daqui a algumas semanas: a Ana (dados de exemplo) atende das 9h às 18h. */
    private static LocalDate segundaFutura(int semanas) {
        return LocalDate.now().plusWeeks(semanas).with(TemporalAdjusters.next(DayOfWeek.MONDAY));
    }

    private JsonNode corpo(MvcResult resultado) throws Exception {
        return json.readTree(resultado.getResponse().getContentAsString());
    }

    private JsonNode agendar(LocalDate dia, String hora, String cliente) throws Exception {
        String pedido = """
                {"profissionalId": 1, "servicoId": 1, "inicio": "%sT%s:00",
                 "clienteNome": "%s", "clienteTelefone": "48999990000", "clienteEmail": "cliente@teste.com"}
                """.formatted(dia, hora, cliente);
        return corpo(mvc.perform(post("/api/agendamentos").contentType(MediaType.APPLICATION_JSON).content(pedido))
                .andExpect(status().isCreated())
                .andReturn());
    }

    private String entrar(String email, String senha) throws Exception {
        String pedido = """
                {"email": "%s", "senha": "%s"}""".formatted(email, senha);
        return corpo(mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(pedido))
                .andExpect(status().isOk())
                .andReturn()).get("token").asText();
    }

    @Test
    void clienteConsultaRemarcaECancelaPeloLink() throws Exception {
        LocalDate dia = segundaFutura(2);
        String codigo = agendar(dia, "10:00", "Cliente do link").get("codigoAcesso").asText();
        assertThat(codigo).hasSize(43);

        mvc.perform(get("/api/meu-agendamento/" + codigo))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.podeAlterar").value(true))
                .andExpect(jsonPath("$.clienteTelefone").doesNotExist());

        mvc.perform(patch("/api/meu-agendamento/" + codigo + "/remarcar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"inicio\": \"%sT11:00:00\"}".formatted(dia)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.inicio").value(dia + "T11:00:00"));

        // O horário antigo ficou livre e o novo, ocupado.
        String horarios = mvc.perform(get("/api/disponibilidade?profissionalId=1&servicoId=1&data=" + dia))
                .andReturn().getResponse().getContentAsString();
        assertThat(horarios).contains("10:00").doesNotContain("11:00");

        mvc.perform(patch("/api/meu-agendamento/" + codigo + "/cancelar"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELADO"));
        mvc.perform(patch("/api/meu-agendamento/" + codigo + "/cancelar"))
                .andExpect(status().isUnprocessableEntity());

        mvc.perform(get("/api/meu-agendamento/codigo-que-nao-existe")).andExpect(status().isNotFound());
    }

    @Test
    void rotasDaEquipeExigemLogin() throws Exception {
        mvc.perform(get("/api/agendamentos?data=" + segundaFutura(1))).andExpect(status().isUnauthorized());
        mvc.perform(put("/api/servicos/1").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void administradorEntraEVeAAgenda() throws Exception {
        String token = entrar("admin@agendamento.local", "admin123");
        mvc.perform(get("/api/agendamentos?data=" + segundaFutura(1)).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    void profissionalVeSoAPropriaAgendaEPerdeOAcessoAoSerDesativado() throws Exception {
        String admin = entrar("admin@agendamento.local", "admin123");

        // Dados de exemplo: Ana (id 1) atende segunda; Bruno (id 2) atende terça.
        LocalDate segunda = segundaFutura(3);
        LocalDate terca = segunda.plusDays(1);
        agendar(segunda, "10:00", "Cliente da Ana");
        String pedidoBruno = """
                {"profissionalId": 2, "servicoId": 1, "inicio": "%sT14:00:00",
                 "clienteNome": "Cliente do Bruno", "clienteTelefone": "48999990000"}
                """.formatted(terca);
        long idBruno = corpo(mvc.perform(post("/api/agendamentos").contentType(MediaType.APPLICATION_JSON)
                .content(pedidoBruno)).andExpect(status().isCreated()).andReturn()).get("id").asLong();

        String novoLogin = """
                {"nome": "Ana", "email": "ana@barbearia.com", "senha": "senha-da-ana",
                 "perfil": "PROFISSIONAL", "profissionalId": 1}""";
        long idLoginAna = corpo(mvc.perform(post("/api/usuarios").header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON).content(novoLogin))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.profissionalNome").value("Ana"))
                .andReturn()).get("id").asLong();

        String ana = entrar("ana@barbearia.com", "senha-da-ana");

        // Pedindo a agenda do Bruno, recebe a própria (vazia na terça).
        mvc.perform(get("/api/agendamentos?profissionalId=2&data=" + terca).header("Authorization", "Bearer " + ana))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
        mvc.perform(get("/api/agendamentos?data=" + segunda).header("Authorization", "Bearer " + ana))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].clienteNome").value("Cliente da Ana"));
        mvc.perform(patch("/api/agendamentos/" + idBruno + "/cancelar").header("Authorization", "Bearer " + ana))
                .andExpect(status().isNotFound());

        // Telas de administrador: proibido.
        mvc.perform(get("/api/usuarios").header("Authorization", "Bearer " + ana)).andExpect(status().isForbidden());
        mvc.perform(get("/api/relatorios/mensal?ano=2026&mes=10").header("Authorization", "Bearer " + ana))
                .andExpect(status().isForbidden());

        // Troca a própria senha.
        mvc.perform(patch("/api/auth/senha").header("Authorization", "Bearer " + ana)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"senhaAtual\": \"senha-da-ana\", \"novaSenha\": \"outra-senha\"}"))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/auth/eu").header("Authorization", "Bearer " + ana))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.perfil").value("PROFISSIONAL"));

        // Desativada pelo administrador: o token que ela já tinha para de valer na hora.
        String desativar = """
                {"nome": "Ana", "email": "ana@barbearia.com", "perfil": "PROFISSIONAL",
                 "profissionalId": 1, "ativo": false}""";
        mvc.perform(put("/api/usuarios/" + idLoginAna).header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON).content(desativar))
                .andExpect(status().isOk());
        mvc.perform(get("/api/agendamentos?data=" + segunda).header("Authorization", "Bearer " + ana))
                .andExpect(status().isUnauthorized());
    }
}
