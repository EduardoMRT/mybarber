package br.com.mybarber.api.agenda;

import br.com.mybarber.api.agenda.CalculadoraHorarios.Intervalo;
import br.com.mybarber.api.agenda.CalculadoraHorarios.Janela;
import br.com.mybarber.api.agenda.CalculadoraHorarios.Parametros;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class CalculadoraHorariosTest {

    private static final LocalDate DIA = LocalDate.of(2026, 10, 13);
    private static final long CORTE = 1L;
    private static final long BARBA = 2L;

    private static Parametros params(int duracao, int intervalo, List<Janela> janelas, List<Intervalo> ocupados,
                                     List<Intervalo> bloqueios, long servico) {
        return new Parametros(DIA, servico, duracao, 15, intervalo, janelas, ocupados, bloqueios, null);
    }

    private static Intervalo i(String ini, String fim) {
        return new Intervalo(DIA.atTime(LocalTime.parse(ini)), DIA.atTime(LocalTime.parse(fim)));
    }

    private static Janela j(String ini, String fim, Long... aceitos) {
        return new Janela(LocalTime.parse(ini), LocalTime.parse(fim), Set.of(aceitos));
    }

    @Test
    void exemploDoProjeto_corteLisoNaoCabeAs1530() {
        // livre as 15:30, cliente marcado as 16:00 -> 30 min de espaco
        List<Janela> janelas = List.of(j("15:30", "18:00"));
        List<Intervalo> ocupados = List.of(i("16:00", "16:30"));

        Parametros liso = params(45, 0, janelas, ocupados, List.of(), CORTE);
        Parametros crespo = params(30, 0, janelas, ocupados, List.of(), CORTE);

        assertThat(CalculadoraHorarios.disponiveis(liso)).doesNotContain(LocalTime.of(15, 30));
        assertThat(CalculadoraHorarios.disponiveis(crespo)).contains(LocalTime.of(15, 30));
    }

    @Test
    void servicoPrecisaCaberAteOFimDaJanela() {
        Parametros p = params(45, 0, List.of(j("09:00", "10:00")), List.of(), List.of(), CORTE);
        assertThat(CalculadoraHorarios.disponiveis(p))
                .containsExactly(LocalTime.of(9, 0), LocalTime.of(9, 15));
    }

    @Test
    void janelaRestritaSoAceitaServicosListados() {
        List<Janela> janelas = List.of(j("18:00", "19:00", BARBA));
        assertThat(CalculadoraHorarios.disponiveis(params(30, 0, janelas, List.of(), List.of(), CORTE))).isEmpty();
        assertThat(CalculadoraHorarios.disponiveis(params(20, 0, janelas, List.of(), List.of(), BARBA))).isNotEmpty();
    }

    @Test
    void intervaloEntreClientesEhRespeitado() {
        List<Janela> janelas = List.of(j("09:00", "12:00"));
        List<Intervalo> ocupados = List.of(i("10:00", "10:30"));
        Parametros p = params(30, 15, janelas, ocupados, List.of(), CORTE);
        List<LocalTime> livres = CalculadoraHorarios.disponiveis(p);
        assertThat(livres).contains(LocalTime.of(9, 15)).doesNotContain(LocalTime.of(9, 30));
        assertThat(livres).doesNotContain(LocalTime.of(10, 30)).contains(LocalTime.of(10, 45));
    }

    @Test
    void bloqueioImpedeHorario() {
        Parametros p = params(30, 0, List.of(j("09:00", "11:00")), List.of(), List.of(i("09:30", "10:00")), CORTE);
        assertThat(CalculadoraHorarios.disponiveis(p))
                .containsExactly(LocalTime.of(9, 0), LocalTime.of(10, 0), LocalTime.of(10, 15), LocalTime.of(10, 30));
    }

    @Test
    void antecedenciaMinimaEscondeHorariosPassados() {
        Parametros p = new Parametros(DIA, CORTE, 30, 15, 0, List.of(j("09:00", "11:00")), List.of(), List.of(),
                LocalDateTime.of(DIA, LocalTime.of(10, 0)));
        assertThat(CalculadoraHorarios.disponiveis(p)).first().isEqualTo(LocalTime.of(10, 0));
    }

    @Test
    void barbeiroPodeSugerirHorarioForaDaGrade() {
        Parametros p = params(30, 0, List.of(j("09:00", "11:00")), List.of(), List.of(), CORTE);
        assertThat(CalculadoraHorarios.ofertado(p, LocalTime.of(9, 10))).isFalse();
        assertThat(CalculadoraHorarios.cabeNaJanela(p, LocalTime.of(9, 10))).isTrue();
        assertThat(CalculadoraHorarios.cabeNaJanela(p, LocalTime.of(10, 40))).isFalse();
    }
}
