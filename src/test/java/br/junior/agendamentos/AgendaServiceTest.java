package br.junior.agendamentos;

import static org.junit.jupiter.api.Assertions.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class AgendaServiceTest {
    @Test void impedeSobreposicao() {
        AgendaService agenda = new AgendaService();
        LocalDateTime inicio = LocalDateTime.of(2026, 11, 3, 9, 0);
        agenda.agendar("João", inicio, inicio.plusHours(1));
        assertFalse(agenda.disponivel(inicio.plusMinutes(30), inicio.plusHours(2)));
        assertThrows(IllegalStateException.class, () -> agenda.agendar("Maria", inicio.plusMinutes(30), inicio.plusHours(2)));
    }
    @Test void permiteHorariosAdjacentes() {
        AgendaService agenda = new AgendaService();
        LocalDateTime inicio = LocalDateTime.of(2026, 11, 3, 9, 0);
        agenda.agendar("João", inicio, inicio.plusHours(1));
        assertTrue(agenda.disponivel(inicio.plusHours(1), inicio.plusHours(2)));
    }

    @Test void cancelamentoLiberaSomenteOHorarioSelecionado() {
        AgendaService agenda = new AgendaService();
        LocalDateTime inicio = LocalDateTime.of(2026, 11, 3, 9, 0);
        Agendamento primeiro = agenda.agendar("João", inicio, inicio.plusHours(1));
        Agendamento segundo = agenda.agendar("Maria", inicio.plusHours(1), inicio.plusHours(2));
        var antes = agenda.listar();

        assertTrue(agenda.cancelar(primeiro));
        assertTrue(agenda.disponivel(inicio, inicio.plusHours(1)));
        assertFalse(agenda.disponivel(segundo.inicio(), segundo.fim()));
        assertEquals(java.util.List.of(segundo), agenda.listar());
        assertEquals(2, antes.size());
        assertFalse(agenda.cancelar(primeiro));
    }

    @Test void cancelarRegistroInexistenteNaoAlteraAgenda() {
        AgendaService agenda = new AgendaService();
        LocalDateTime inicio = LocalDateTime.of(2026, 11, 3, 9, 0);
        Agendamento existente = agenda.agendar("João", inicio, inicio.plusHours(1));

        assertFalse(agenda.cancelar(new Agendamento("Outra pessoa", inicio, inicio.plusHours(1))));
        assertThrows(NullPointerException.class, () -> agenda.cancelar(null));
        assertEquals(java.util.List.of(existente), agenda.listar());
    }

    @Test void rejeitaIntervalosInvalidosSemAlterarAgenda() {
        AgendaService agenda = new AgendaService();
        LocalDateTime inicio = LocalDateTime.of(2026, 11, 3, 9, 0);
        assertThrows(IllegalArgumentException.class, () -> agenda.agendar("João", inicio, inicio));
        assertThrows(IllegalArgumentException.class, () -> agenda.agendar("João", inicio, inicio.minusMinutes(1)));
        assertThrows(IllegalArgumentException.class, () -> agenda.disponivel(inicio, inicio));
        assertThrows(IllegalArgumentException.class, () -> agenda.agendar("  ", inicio, inicio.plusHours(1)));
        assertTrue(agenda.listar().isEmpty());
    }
    @Test void listaPorDiaIncluiReservasQueAtravessamMeiaNoite() {
        AgendaService agenda = new AgendaService();
        LocalDate dia = LocalDate.of(2026, 11, 11);
        var cedo = agenda.agendar("A", dia.atTime(9, 0), dia.atTime(10, 0));
        var noite = agenda.agendar("B", dia.atTime(23, 30), dia.plusDays(1).atTime(0, 30));
        var anterior = agenda.agendar("C", dia.minusDays(1).atTime(23, 30), dia.atTime(0, 30));
        agenda.agendar("D", dia.plusDays(1).atTime(1, 0), dia.plusDays(1).atTime(2, 0));
        assertEquals(java.util.List.of(anterior, cedo, noite), agenda.listarPorDia(dia));
        assertEquals(java.util.List.of(noite), agenda.listarPorDia(dia.plusDays(1)).stream()
                .filter(a -> a.equals(noite)).toList());
    }

    @Test void listaPorDiaRetornaSnapshotImutavelEValidaDia() {
        AgendaService agenda = new AgendaService();
        LocalDate dia = LocalDate.of(2026, 11, 11);
        var snapshot = agenda.listarPorDia(dia);
        assertTrue(snapshot.isEmpty());
        assertThrows(NullPointerException.class, () -> agenda.listarPorDia(null));
        assertThrows(UnsupportedOperationException.class, () -> snapshot.add(
                new Agendamento("X", dia.atTime(10, 0), dia.atTime(11, 0))));
        agenda.agendar("X", dia.atTime(10, 0), dia.atTime(11, 0));
        assertTrue(snapshot.isEmpty());
        assertEquals(1, agenda.listarPorDia(dia).size());
    }
}
