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
}
