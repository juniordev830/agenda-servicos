package br.junior.agendamentos;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** Agenda semanal com feriados, pausas e bloqueios excepcionais por profissional. */
public final class CalendarioTrabalho {
    public record Expediente(LocalTime abertura, LocalTime fechamento) {
        public Expediente {
            Objects.requireNonNull(abertura, "abertura");
            Objects.requireNonNull(fechamento, "fechamento");
            if (!fechamento.isAfter(abertura)) {
                throw new IllegalArgumentException("expediente precisa terminar no mesmo dia");
            }
        }

        public boolean contem(LocalTime inicio, LocalTime fim) {
            return !inicio.isBefore(abertura) && !fim.isAfter(fechamento);
        }

        public long minutos() {
            return Duration.between(abertura, fechamento).toMinutes();
        }
    }

    private final Map<DayOfWeek, List<Expediente>> semana = new EnumMap<>(DayOfWeek.class);
    private final Set<LocalDate> feriados = new HashSet<>();
    private final List<Intervalo> bloqueios = new ArrayList<>();

    public CalendarioTrabalho() {
        for (DayOfWeek dia : DayOfWeek.values()) {
            semana.put(dia, List.of());
        }
    }

    public synchronized void definirExpediente(DayOfWeek dia, List<Expediente> periodos) {
        Objects.requireNonNull(dia, "dia");
        Objects.requireNonNull(periodos, "periodos");
        List<Expediente> ordenados = new ArrayList<>(periodos);
        if (ordenados.contains(null)) {
            throw new IllegalArgumentException("periodo nulo");
        }
        ordenados.sort(Comparator.comparing(Expediente::abertura));
        for (int i = 1; i < ordenados.size(); i++) {
            if (ordenados.get(i).abertura().isBefore(ordenados.get(i - 1).fechamento())) {
                throw new IllegalArgumentException("expedientes sobrepostos");
            }
        }
        semana.put(dia, List.copyOf(ordenados));
    }

    public synchronized void definirDiasUteis(LocalTime abertura, LocalTime fechamento) {
        Expediente periodo = new Expediente(abertura, fechamento);
        for (DayOfWeek dia : DayOfWeek.values()) {
            definirExpediente(dia, dia == DayOfWeek.SATURDAY || dia == DayOfWeek.SUNDAY
                    ? List.of() : List.of(periodo));
        }
    }

    public synchronized List<Expediente> expedientes(DayOfWeek dia) {
        return semana.get(Objects.requireNonNull(dia, "dia"));
    }

    public synchronized void adicionarFeriado(LocalDate dia) {
        feriados.add(Objects.requireNonNull(dia, "dia"));
    }

    public synchronized boolean removerFeriado(LocalDate dia) {
        return feriados.remove(Objects.requireNonNull(dia, "dia"));
    }

    public synchronized boolean feriado(LocalDate dia) {
        return feriados.contains(Objects.requireNonNull(dia, "dia"));
    }

    public synchronized void bloquear(Intervalo intervalo) {
        Objects.requireNonNull(intervalo, "intervalo");
        if (bloqueios.contains(intervalo)) {
            throw new IllegalArgumentException("bloqueio duplicado");
        }
        bloqueios.add(intervalo);
        bloqueios.sort(Comparator.naturalOrder());
    }

    public synchronized boolean desbloquear(Intervalo intervalo) {
        return bloqueios.remove(Objects.requireNonNull(intervalo, "intervalo"));
    }

    public synchronized List<Intervalo> bloqueios() {
        return List.copyOf(bloqueios);
    }

    public synchronized boolean permite(Intervalo intervalo) {
        Objects.requireNonNull(intervalo, "intervalo");
        LocalDate dia = intervalo.inicio().toLocalDate();
        if (!dia.equals(intervalo.fim().toLocalDate()) || feriados.contains(dia)) {
            return false;
        }
        boolean dentro = semana.get(dia.getDayOfWeek()).stream()
                .anyMatch(p -> p.contem(intervalo.inicio().toLocalTime(), intervalo.fim().toLocalTime()));
        if (!dentro) {
            return false;
        }
        return bloqueios.stream().noneMatch(intervalo::conflita);
    }

    public synchronized List<Intervalo> gerarCandidatos(LocalDate dia, Duration duracao, Duration passo) {
        Objects.requireNonNull(dia, "dia");
        validarDuracao(duracao, "duracao");
        validarDuracao(passo, "passo");
        if (feriados.contains(dia)) {
            return List.of();
        }
        List<Intervalo> candidatos = new ArrayList<>();
        for (Expediente periodo : semana.get(dia.getDayOfWeek())) {
            LocalDateTime cursor = dia.atTime(periodo.abertura());
            LocalDateTime limite = dia.atTime(periodo.fechamento());
            while (!cursor.plus(duracao).isAfter(limite)) {
                Intervalo intervalo = new Intervalo(cursor, cursor.plus(duracao));
                if (permite(intervalo)) {
                    candidatos.add(intervalo);
                }
                cursor = cursor.plus(passo);
            }
        }
        return List.copyOf(candidatos);
    }

    public synchronized long minutosDeExpediente(LocalDate dia) {
        if (feriados.contains(Objects.requireNonNull(dia, "dia"))) {
            return 0;
        }
        return semana.get(dia.getDayOfWeek()).stream().mapToLong(Expediente::minutos).sum();
    }

    private static void validarDuracao(Duration duracao, String nome) {
        Objects.requireNonNull(duracao, nome);
        if (duracao.isNegative() || duracao.isZero() || duracao.compareTo(Duration.ofHours(24)) > 0
                || duracao.getSeconds() % 60 != 0) {
            throw new IllegalArgumentException(nome + " deve ter minutos inteiros entre 1 e 1440");
        }
    }
}
