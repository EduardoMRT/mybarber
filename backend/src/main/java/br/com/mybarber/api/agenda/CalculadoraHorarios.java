package br.com.mybarber.api.agenda;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * Regra central do MyBarber: um horario so e oferecido se o servico couber inteiro
 * (RN02) dentro de uma janela de trabalho que aceite aquele servico (RN03), sem
 * invadir outro atendimento (respeitando o intervalo entre clientes, RN05) nem um
 * bloqueio, e respeitando a antecedencia minima.
 *
 * Classe pura (sem Spring) para ser testada isoladamente.
 */
public final class CalculadoraHorarios {

    private CalculadoraHorarios() {
    }

    /** Intervalo de tempo ocupado [inicio, fim). */
    public record Intervalo(LocalDateTime inicio, LocalDateTime fim) {
    }

    /** Janela de trabalho de um dia. servicosAceitos vazio = aceita todos os servicos. */
    public record Janela(LocalTime inicio, LocalTime fim, Set<Long> servicosAceitos) {
        public boolean aceita(long servicoId) {
            return servicosAceitos == null || servicosAceitos.isEmpty() || servicosAceitos.contains(servicoId);
        }
    }

    public record Parametros(
            LocalDate data,
            long servicoId,
            int duracaoMin,
            int gradeMin,
            int intervaloMin,
            List<Janela> janelas,
            List<Intervalo> agendamentos,
            List<Intervalo> bloqueios,
            LocalDateTime naoAntesDe) {
    }

    /** Passo do "encaixe colado": horarios colados num atendimento sao arredondados para multiplos de 5 min. */
    static final int PASSO_ENCAIXE = 5;

    /**
     * Horarios de inicio oferecidos ao cliente.
     *
     * Alem da grade da barbearia (RN04, ex.: de 15 em 15 min a partir do inicio da faixa),
     * oferece tambem os horarios "colados": logo depois de cada atendimento ou bloqueio
     * (fim + intervalo) e logo antes do proximo, arredondados para 5 min. Assim um vao que
     * nao comeca na grade (ex.: 13:02 depois de uma barba) nao e desperdicado.
     */
    public static List<LocalTime> disponiveis(Parametros p) {
        TreeSet<LocalTime> resultado = new TreeSet<>();
        int grade = Math.max(5, p.gradeMin());
        int dur = p.duracaoMin();
        int gap = Math.max(0, p.intervaloMin());
        LocalDateTime meiaNoite = p.data().atStartOfDay();

        // pontos colados em atendimentos e bloqueios (minutos desde a meia-noite do dia)
        TreeSet<Integer> colados = new TreeSet<>();
        for (Intervalo o : p.agendamentos()) {
            colados.add(acima(minutosDesde(meiaNoite, o.fim()) + gap));
            colados.add(abaixo(minutosDesde(meiaNoite, o.inicio()) - gap - dur));
        }
        for (Intervalo b : p.bloqueios()) {
            colados.add(acima(minutosDesde(meiaNoite, b.fim())));
            colados.add(abaixo(minutosDesde(meiaNoite, b.inicio()) - dur));
        }

        for (Janela janela : p.janelas()) {
            if (!janela.aceita(p.servicoId())) {
                continue;
            }
            int ini = minutos(janela.inicio());
            int fim = minutosFim(janela.fim());
            TreeSet<Integer> candidatos = new TreeSet<>();
            for (int t = ini; t + dur <= fim; t += grade) {
                candidatos.add(t);
            }
            candidatos.add(abaixo(fim - dur)); // ultimo horario que ainda cabe no fim da faixa
            candidatos.addAll(colados);
            for (int t : candidatos) {
                if (t < ini || t + dur > fim) {
                    continue;
                }
                LocalDateTime inicio = meiaNoite.plusMinutes(t);
                if (livre(p, inicio)) {
                    resultado.add(inicio.toLocalTime());
                }
            }
        }
        return new ArrayList<>(resultado);
    }

    private static int minutosDesde(LocalDateTime base, LocalDateTime t) {
        return (int) java.time.Duration.between(base, t).toMinutes();
    }

    private static int acima(int m) {
        return Math.floorDiv(m + PASSO_ENCAIXE - 1, PASSO_ENCAIXE) * PASSO_ENCAIXE;
    }

    private static int abaixo(int m) {
        return Math.floorDiv(m, PASSO_ENCAIXE) * PASSO_ENCAIXE;
    }

    /** O cliente so pode reservar um horario que esteja na lista oferecida. */
    public static boolean ofertado(Parametros p, LocalTime inicio) {
        return disponiveis(p).contains(inicio);
    }

    /**
     * Usado quando o barbeiro sugere um horario (remarcacao): precisa estar dentro de
     * uma janela que aceite o servico e livre, mas nao precisa estar alinhado a grade.
     */
    public static boolean cabeNaJanela(Parametros p, LocalTime inicio) {
        int t = minutos(inicio);
        boolean dentro = p.janelas().stream()
                .filter(j -> j.aceita(p.servicoId()))
                .anyMatch(j -> t >= minutos(j.inicio()) && t + p.duracaoMin() <= minutosFim(j.fim()));
        return dentro && livre(p, p.data().atTime(inicio));
    }

    /** Sem conflito com outros atendimentos e bloqueios (ignora janelas e antecedencia). */
    public static boolean semConflito(Parametros p, LocalDateTime inicio) {
        LocalDateTime fim = inicio.plusMinutes(p.duracaoMin());
        return semConflitoAgendamentos(p, inicio, fim) && semConflitoBloqueios(p, inicio, fim);
    }

    private static boolean livre(Parametros p, LocalDateTime inicio) {
        if (p.naoAntesDe() != null && inicio.isBefore(p.naoAntesDe())) {
            return false;
        }
        return semConflito(p, inicio);
    }

    private static boolean semConflitoAgendamentos(Parametros p, LocalDateTime inicio, LocalDateTime fim) {
        int gap = Math.max(0, p.intervaloMin());
        for (Intervalo o : p.agendamentos()) {
            // conflito se [inicio, fim+gap) intersecta [o.inicio - 0, o.fim + gap)
            if (inicio.isBefore(o.fim().plusMinutes(gap)) && fim.plusMinutes(gap).isAfter(o.inicio())) {
                return false;
            }
        }
        return true;
    }

    private static boolean semConflitoBloqueios(Parametros p, LocalDateTime inicio, LocalDateTime fim) {
        for (Intervalo b : p.bloqueios()) {
            if (inicio.isBefore(b.fim()) && fim.isAfter(b.inicio())) {
                return false;
            }
        }
        return true;
    }

    private static int minutos(LocalTime t) {
        return t.getHour() * 60 + t.getMinute();
    }

    /** 00:00 como fim de janela significa meia-noite (fim do dia). */
    private static int minutosFim(LocalTime t) {
        int m = minutos(t);
        return m == 0 ? 24 * 60 : m;
    }
}
