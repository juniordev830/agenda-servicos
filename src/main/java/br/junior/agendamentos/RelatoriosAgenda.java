package br.junior.agendamentos;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Relatórios imutáveis e exportação de agenda para planilhas e calendários. */
public final class RelatoriosAgenda {
    private static final DateTimeFormatter ICS = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss");
    private static final String CSV_HEADER =
            "id,profissional,servico,cliente,inicio,fim,preco_centavos,estado";

    private RelatoriosAgenda() {}

    public record Indicadores(String profissional, long confirmadas, long canceladas,
                              long minutosReservados, long receitaPrevistaCentavos) {}

    public static Map<String, Indicadores> porProfissional(List<AgendaProfissionais.Reserva> reservas) {
        Objects.requireNonNull(reservas, "reservas");
        Map<String, List<AgendaProfissionais.Reserva>> grupos = new LinkedHashMap<>();
        for (AgendaProfissionais.Reserva reserva : reservas) {
            Objects.requireNonNull(reserva, "reserva");
            grupos.computeIfAbsent(reserva.profissional(), ignorado -> new ArrayList<>()).add(reserva);
        }
        Map<String, Indicadores> resultado = new LinkedHashMap<>();
        for (Map.Entry<String, List<AgendaProfissionais.Reserva>> entrada : grupos.entrySet()) {
            long confirmadas = 0;
            long canceladas = 0;
            long minutos = 0;
            long receita = 0;
            for (AgendaProfissionais.Reserva reserva : entrada.getValue()) {
                if (reserva.ativa()) {
                    confirmadas++;
                    minutos = Math.addExact(minutos, reserva.horario().duracao().toMinutes());
                    receita = Math.addExact(receita, reserva.precoCentavos());
                } else {
                    canceladas++;
                }
            }
            resultado.put(entrada.getKey(), new Indicadores(entrada.getKey(),
                    confirmadas, canceladas, minutos, receita));
        }
        return Map.copyOf(resultado);
    }

    public static Map<LocalDate, Long> demandaDiaria(List<AgendaProfissionais.Reserva> reservas) {
        Objects.requireNonNull(reservas, "reservas");
        Map<LocalDate, Long> contagens = new LinkedHashMap<>();
        reservas.stream().filter(AgendaProfissionais.Reserva::ativa)
                .sorted(Comparator.comparing(r -> r.horario().inicio()))
                .forEach(r -> contagens.merge(r.horario().inicio().toLocalDate(), 1L, Long::sum));
        return Map.copyOf(contagens);
    }

    public static String csv(List<AgendaProfissionais.Reserva> reservas) {
        Objects.requireNonNull(reservas, "reservas");
        StringBuilder saida = new StringBuilder(CSV_HEADER).append("\r\n");
        for (AgendaProfissionais.Reserva reserva : reservas) {
            Objects.requireNonNull(reserva, "reserva");
            saida.append(csvCampo(reserva.id())).append(',')
                    .append(csvCampo(reserva.profissional())).append(',')
                    .append(csvCampo(reserva.codigoServico())).append(',')
                    .append(csvCampo(reserva.cliente())).append(',')
                    .append(csvCampo(reserva.horario().inicio().toString())).append(',')
                    .append(csvCampo(reserva.horario().fim().toString())).append(',')
                    .append(reserva.precoCentavos()).append(',')
                    .append(reserva.estado()).append("\r\n");
        }
        return saida.toString();
    }

    /**
     * Protege a abertura em planilhas contra fórmulas, sem alterar o objeto original.
     * Aspas, quebras de linha e vírgulas seguem a codificação CSV RFC 4180.
     */
    private static String csvCampo(String valor) {
        String seguro = valor;
        if (!seguro.isEmpty() && "=+-@\t\r".indexOf(seguro.charAt(0)) >= 0) {
            seguro = "'" + seguro;
        }
        return "\"" + seguro.replace("\"", "\"\"") + "\"";
    }

    /**
     * Gera um calendário iCalendar local (sem TZID). O consumidor deve interpretar
     * os horários no fuso configurado externamente; não inventamos deslocamento UTC.
     */
    public static String ics(List<AgendaProfissionais.Reserva> reservas, String nomeCalendario) {
        Objects.requireNonNull(reservas, "reservas");
        Objects.requireNonNull(nomeCalendario, "nomeCalendario");
        StringBuilder saida = new StringBuilder();
        saida.append("BEGIN:VCALENDAR\r\nVERSION:2.0\r\nPRODID:-//Agenda Servicos//PT-BR\r\n");
        saida.append("CALSCALE:GREGORIAN\r\nX-WR-CALNAME:")
                .append(escaparIcs(nomeCalendario)).append("\r\n");
        for (AgendaProfissionais.Reserva reserva : reservas) {
            if (!reserva.ativa()) {
                continue;
            }
            saida.append("BEGIN:VEVENT\r\n")
                    .append("UID:").append(escaparIcs(reserva.id())).append("@agenda-servicos\r\n")
                    .append("DTSTART:").append(formatar(reserva.horario().inicio())).append("\r\n")
                    .append("DTEND:").append(formatar(reserva.horario().fim())).append("\r\n")
                    .append("SUMMARY:").append(escaparIcs(reserva.codigoServico())).append("\r\n")
                    .append("DESCRIPTION:").append(escaparIcs("Cliente: " + reserva.cliente()
                            + " | Profissional: " + reserva.profissional())).append("\r\n")
                    .append("END:VEVENT\r\n");
        }
        return saida.append("END:VCALENDAR\r\n").toString();
    }

    private static String formatar(LocalDateTime data) {
        return ICS.format(data);
    }

    private static String escaparIcs(String valor) {
        return valor.replace("\\", "\\\\").replace("\r", "")
                .replace("\n", "\\n").replace(",", "\\,").replace(";", "\\;");
    }

    public static List<AgendaProfissionais.Reserva> proximas(
            List<AgendaProfissionais.Reserva> reservas, LocalDateTime referencia, int limite) {
        Objects.requireNonNull(reservas, "reservas");
        Objects.requireNonNull(referencia, "referencia");
        if (limite < 0 || limite > 10_000) {
            throw new IllegalArgumentException("limite invalido");
        }
        return reservas.stream().filter(AgendaProfissionais.Reserva::ativa)
                .filter(r -> !r.horario().inicio().isBefore(referencia))
                .sorted(Comparator.comparing((AgendaProfissionais.Reserva r) -> r.horario().inicio())
                        .thenComparing(AgendaProfissionais.Reserva::id))
                .limit(limite).toList();
    }
}
