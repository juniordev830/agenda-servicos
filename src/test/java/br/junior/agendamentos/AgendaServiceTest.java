package br.junior.agendamentos;

import static org.junit.jupiter.api.Assertions.*;
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
}
