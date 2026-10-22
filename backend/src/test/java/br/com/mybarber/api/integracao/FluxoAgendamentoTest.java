package br.com.mybarber.api.integracao;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.time.LocalDate;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Fluxo completo contra um MySQL real (no CI, o servico mysql do GitHub Actions):
 * admin cria barbearia -> dono cadastra servico e horarios -> cliente agenda respeitando
 * a regra de encaixe -> barbeiro remarca -> cliente aceita -> bloqueio da barbearia.
 */
@Tag("integracao")
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class FluxoAgendamentoTest {

    @Autowired
    MockMvc mvc;

    private static final long LISO = 1L;

    @Test
    void fluxoCompleto() throws Exception {
        String sufixo = String.valueOf(System.nanoTime());
        LocalDate dia = LocalDate.now(ZoneId.of("America/Sao_Paulo")).plusDays(3);

        // 1. super admin cria a barbearia com o dono que tambem atende
        String admin = login("admin@teste.mybarber", "Admin12345");
        MvcResult criada = mvc.perform(auth(post("/api/admin/barbearias"), admin).content("""
                        {"dados":{"nome":"Barbearia Teste %s","cidade":"Ourinhos","uf":"SP",
                                  "latitude":-22.9789,"longitude":-49.8697,"gradeMinutos":15,
                                  "antecedenciaMinimaMin":30,"prazoCancelamentoMin":120,"prazoRespostaRemarcacaoHoras":12},
                         "cobranca":{"valorMensalidade":89.00,"diaVencimento":10,"diasTolerancia":5},
                         "donoNome":"Joao Barbeiro","donoEmail":"dono%s@teste.com","donoSenha":"Senha12345","donoAtende":true}
                        """.formatted(sufixo, sufixo)))
                .andExpect(status().isCreated()).andReturn();
        long barbeariaId = id(criada, "$.id");

        // 2. dono entra no portal, cadastra servico e horarios
        String dono = login("dono" + sufixo + "@teste.com", "Senha12345");
        MvcResult barbeiros = mvc.perform(auth(get("/api/painel/barbeiros"), dono))
                .andExpect(status().isOk()).andReturn();
        long barbeiroId = id(barbeiros, "$[0].id");

        MvcResult servico = mvc.perform(auth(post("/api/painel/servicos"), dono).content("""
                        {"nome":"Corte","preco":40.00,"duracaoMin":30,"duracoes":[{"tipoCabeloId":1,"duracaoMin":45}]}
                        """)).andExpect(status().isCreated()).andReturn();
        long servicoId = id(servico, "$.id");

        StringBuilder janelas = new StringBuilder();
        for (int d = 1; d <= 7; d++) {
            if (d > 1) janelas.append(',');
            janelas.append("{\"diaSemana\":%d,\"inicio\":\"09:00\",\"fim\":\"18:00\"}".formatted(d));
        }
        mvc.perform(auth(put("/api/painel/barbeiros/" + barbeiroId + "/horarios"), dono)
                        .content("{\"janelas\":[" + janelas + "]}"))
                .andExpect(status().isOk());

        // 3. cliente com cabelo liso se cadastra
        MvcResult reg = mvc.perform(post("/api/auth/registrar").contentType(MediaType.APPLICATION_JSON).content("""
                        {"nome":"Cliente Teste","email":"cliente%s@teste.com","senha":"Senha12345","tipoCabeloId":1,
                         "aceiteTermos":true,"aceitePrivacidade":true}
                        """.formatted(sufixo)))
                .andExpect(status().isCreated()).andReturn();
        String cliente = JsonPath.read(reg.getResponse().getContentAsString(), "$.accessToken");

        // 4. barbearia aparece na busca por proximidade
        mvc.perform(get("/api/public/barbearias").param("lat", "-22.98").param("lng", "-49.87"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == " + barbeariaId + ")]").exists());

        // 5. corte em cabelo liso dura 45 min
        mvc.perform(get("/api/public/horarios").param("barbeiroId", "" + barbeiroId)
                        .param("servicoId", "" + servicoId).param("tipoCabeloId", "" + LISO).param("data", dia.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.duracaoMin").value(45));

        // 6. barbeiro lanca um atendimento manual as 10:00
        mvc.perform(auth(post("/api/painel/agendamentos"), dono).content("""
                        {"barbeiroId":%d,"servicoId":%d,"clienteNome":"Cliente balcao","inicio":"%sT10:00:00"}
                        """.formatted(barbeiroId, servicoId, dia)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.origem").value("MANUAL"));

        // 7. regra de encaixe: 09:30 + 45 min invade as 10:00 -> recusado
        mvc.perform(auth(post("/api/app/agendamentos"), cliente).content("""
                        {"barbeiroId":%d,"servicoId":%d,"inicio":"%sT09:30:00"}
                        """.formatted(barbeiroId, servicoId, dia)))
                .andExpect(status().isConflict());

        // 8. 09:00 cabe (termina 09:45)
        MvcResult ag = mvc.perform(auth(post("/api/app/agendamentos"), cliente).content("""
                        {"barbeiroId":%d,"servicoId":%d,"inicio":"%sT09:00:00"}
                        """.formatted(barbeiroId, servicoId, dia)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("CONFIRMADO"))
                .andExpect(jsonPath("$.duracaoMin").value(45))
                .andReturn();
        long agendamentoId = id(ag, "$.id");

        // 9. barbeiro remarca para 15:00
        mvc.perform(auth(post("/api/painel/agendamentos/" + agendamentoId + "/remarcar"), dono).content("""
                        {"novoInicio":"%sT15:00:00","motivo":"Imprevisto na agenda"}
                        """.formatted(dia)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("AGUARDANDO_CLIENTE"))
                .andExpect(jsonPath("$.remarcacao.motivo").value("Imprevisto na agenda"));

        // 10. cliente ve o aviso e aceita
        mvc.perform(auth(get("/api/app/notificacoes"), cliente))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itens[0].tipo").value("REMARCACAO_PROPOSTA"));
        mvc.perform(auth(post("/api/app/agendamentos/" + agendamentoId + "/remarcacao/aceitar"), cliente))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMADO"))
                .andExpect(jsonPath("$.inicio").value(dia + "T15:00:00"));

        // 11. dashboard do dono responde
        mvc.perform(auth(get("/api/painel/dashboard"), dono)).andExpect(status().isOk());

        // 12. cliente nao acessa o portal; barbeiro nao acessa o admin
        mvc.perform(auth(get("/api/painel/barbeiros"), cliente)).andExpect(status().isForbidden());
        mvc.perform(auth(get("/api/admin/dashboard"), dono)).andExpect(status().isForbidden());

        // 13. admin bloqueia: portal fica so leitura e a barbearia some do app
        mvc.perform(auth(post("/api/admin/barbearias/" + barbeariaId + "/bloquear"), admin)
                        .content("{\"motivo\":\"Teste\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("BLOQUEADA"));
        mvc.perform(auth(post("/api/painel/servicos"), dono).content("""
                        {"nome":"Barba","preco":25.00,"duracaoMin":20}
                        """)).andExpect(status().isLocked());
        mvc.perform(get("/api/public/barbearias/" + barbeariaId)).andExpect(status().isNotFound());

        // 14. LGPD: exportar e excluir conta
        mvc.perform(auth(get("/api/app/meus-dados"), cliente))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.consentimentos.length()").value(2));
        mvc.perform(auth(post("/api/app/excluir-conta"), cliente).content("{\"senha\":\"Senha12345\"}"))
                .andExpect(status().isNoContent());
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"cliente" + sufixo + "@teste.com\",\"senha\":\"Senha12345\"}"))
                .andExpect(status().isUnauthorized());
    }

    private String login(String email, String senha) throws Exception {
        MvcResult r = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + email + "\",\"senha\":\"" + senha + "\"}"))
                .andExpect(status().isOk()).andReturn();
        String token = JsonPath.read(r.getResponse().getContentAsString(), "$.accessToken");
        assertThat(token).isNotBlank();
        return token;
    }

    private static MockHttpServletRequestBuilder auth(MockHttpServletRequestBuilder req, String token) {
        return req.header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON);
    }

    private static long id(MvcResult r, String caminho) throws Exception {
        Number n = JsonPath.read(r.getResponse().getContentAsString(), caminho);
        return n.longValue();
    }
}
