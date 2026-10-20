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
}
