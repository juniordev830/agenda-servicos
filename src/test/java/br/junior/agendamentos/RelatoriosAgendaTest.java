package br.junior.agendamentos;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

class RelatoriosAgendaTest {
    private final LocalDate segunda = LocalDate.of(2026, 11, 9);

    private AgendaProfissionais.Reserva reserva(String id, String profissional, String cliente,
                                                 int hora, long preco,
                                                 AgendaProfissionais.Estado estado) {
        LocalDateTime inicio = segunda.atTime(hora, 0);
        return new AgendaProfissionais.Reserva(id, profissional, "consulta", cliente,
                new Intervalo(inicio, inicio.plus(Duration.ofMinutes(30))), preco, estado);
    }

    @Test void agrupamentoConsideraSomenteReceitaConfirmada() {
        var uma = reserva("1", "ana", "Maria", 9, 2500, AgendaProfissionais.Estado.CONFIRMADA);
        var duas = reserva("2", "ana", "Pedro", 10, 3000, AgendaProfissionais.Estado.CANCELADA);
        var tres = reserva("3", "bia", "Carla", 11, 4500, AgendaProfissionais.Estado.CONFIRMADA);
        var resumo = RelatoriosAgenda.porProfissional(List.of(uma, duas, tres));
        assertEquals(2, resumo.size());
        assertEquals(1, resumo.get("ana").confirmadas());
        assertEquals(1, resumo.get("ana").canceladas());
        assertEquals(30, resumo.get("ana").minutosReservados());
        assertEquals(2500, resumo.get("ana").receitaPrevistaCentavos());
        assertEquals(4500, resumo.get("bia").receitaPrevistaCentavos());
        assertThrows(UnsupportedOperationException.class, () -> resumo.clear());
    }

    @Test void demandaDiariaIgnoraCancelamentos() {
        var uma = reserva("1", "ana", "Maria", 9, 2500, AgendaProfissionais.Estado.CONFIRMADA);
        var duas = reserva("2", "ana", "Pedro", 10, 3000, AgendaProfissionais.Estado.CANCELADA);
        var tres = reserva("3", "bia", "Carla", 11, 4500, AgendaProfissionais.Estado.CONFIRMADA);
        var contagens = RelatoriosAgenda.demandaDiaria(List.of(uma, duas, tres));
        assertEquals(1, contagens.size());
        assertEquals(2L, contagens.get(segunda));
        assertTrue(RelatoriosAgenda.demandaDiaria(List.of(duas)).isEmpty());
    }

    @Test void csvEscapaAspasVirgulasQuebrasEFolhasDeCalculo() {
        var reserva = reserva("1", "ana", "=SUM(1,1),\"teste\"\nLinha 2",
                9, 2500, AgendaProfissionais.Estado.CONFIRMADA);
        String csv = RelatoriosAgenda.csv(List.of(reserva));
        assertTrue(csv.startsWith("id,profissional,servico,cliente,inicio,fim,preco_centavos,estado\r\n"));
        assertTrue(csv.contains("\"'=SUM(1,1),\"\"teste\"\"\nLinha 2\""));
        assertTrue(csv.contains(",2500,CONFIRMADA\r\n"));
        assertEquals(1, csv.split("CONFIRMADA").length - 1);
        assertEquals(1, RelatoriosAgenda.csv(List.of()).split("\r\n").length);
    }

    @Test void calendarioIcsEscapaTextoENaoIncluiCanceladas() {
        var uma = reserva("1", "ana", "Maria,Silva;Teste\nNova",
                9, 2500, AgendaProfissionais.Estado.CONFIRMADA);
        var cancelada = reserva("2", "ana", "Pedro", 10,
                3000, AgendaProfissionais.Estado.CANCELADA);
        String ics = RelatoriosAgenda.ics(List.of(uma, cancelada), "Agenda, Profissionais");
        assertTrue(ics.startsWith("BEGIN:VCALENDAR\r\nVERSION:2.0\r\n"));
        assertTrue(ics.contains("X-WR-CALNAME:Agenda\\, Profissionais"));
        assertTrue(ics.contains("DTSTART:20261109T090000"));
        assertTrue(ics.contains("DTEND:20261109T093000"));
        assertTrue(ics.contains("Maria\\,Silva\\;Teste\\nNova"));
        assertTrue(ics.contains("UID:1@agenda-servicos"));
        assertFalse(ics.contains("UID:2@agenda-servicos"));
        assertTrue(ics.endsWith("END:VCALENDAR\r\n"));
    }

    @Test void proximasOrdenaLimitaEIgnoraCanceladas() {
        var onze = reserva("3", "ana", "Maria", 11,
                2500, AgendaProfissionais.Estado.CONFIRMADA);
        var nove = reserva("1", "ana", "Pedro", 9,
                2500, AgendaProfissionais.Estado.CONFIRMADA);
        var dezCancelada = reserva("2", "ana", "Carlos", 10,
                2500, AgendaProfissionais.Estado.CANCELADA);
        var lista = List.of(onze, dezCancelada, nove);
        assertEquals(List.of("1", "3"), RelatoriosAgenda.proximas(
                lista, segunda.atTime(8, 0), 10).stream().map(r -> r.id()).toList());
        assertEquals(List.of("3"), RelatoriosAgenda.proximas(
                lista, segunda.atTime(10, 0), 1).stream().map(r -> r.id()).toList());
        assertTrue(RelatoriosAgenda.proximas(lista, segunda.atTime(8, 0), 0).isEmpty());
        assertThrows(IllegalArgumentException.class, () -> RelatoriosAgenda.proximas(
                lista, segunda.atTime(8, 0), -1));
        assertThrows(IllegalArgumentException.class, () -> RelatoriosAgenda.proximas(
                lista, segunda.atTime(8, 0), 10001));
    }

    @Test void entradasNulasSaoRejeitadas() {
        assertThrows(NullPointerException.class, () -> RelatoriosAgenda.csv(null));
        assertThrows(NullPointerException.class, () -> RelatoriosAgenda.ics(null, "agenda"));
        assertThrows(NullPointerException.class, () -> RelatoriosAgenda.ics(List.of(), null));
        assertThrows(NullPointerException.class, () -> RelatoriosAgenda.demandaDiaria(null));
        assertThrows(NullPointerException.class, () -> RelatoriosAgenda.porProfissional(null));
    }
}
