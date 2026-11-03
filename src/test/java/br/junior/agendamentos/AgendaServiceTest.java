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

    @Test void remarcarLiberaHorarioAnteriorSemCriarRegistroExtra() {
        AgendaService agenda = new AgendaService();
        var inicio = LocalDateTime.of(2026, 11, 3, 9, 0);
        var original = agenda.agendar("Cliente", inicio, inicio.plusHours(1));
        var remarcado = agenda.reagendar(original, inicio.plusHours(2), inicio.plusHours(3));

        assertEquals("Cliente", remarcado.cliente());
        assertTrue(agenda.disponivel(inicio, inicio.plusHours(1)));
        assertFalse(agenda.disponivel(remarcado.inicio(), remarcado.fim()));
        assertEquals(java.util.List.of(remarcado), agenda.listar());
        assertFalse(agenda.cancelar(original));
        assertTrue(agenda.cancelar(remarcado));
    }

    @Test void falhaNaRemarcacaoPreservaReservasEHorariosOriginais() {
        AgendaService agenda = new AgendaService();
        var inicio = LocalDateTime.of(2026, 11, 3, 9, 0);
        var primeiro = agenda.agendar("Ana", inicio, inicio.plusHours(1));
        var segundo = agenda.agendar("Bruno", inicio.plusHours(1), inicio.plusHours(2));
        var snapshot = agenda.listar();

        assertThrows(IllegalStateException.class,
                () -> agenda.reagendar(primeiro, inicio.plusHours(1), inicio.plusHours(2)));
        assertThrows(IllegalArgumentException.class,
                () -> agenda.reagendar(primeiro, inicio, inicio));
        assertThrows(IllegalArgumentException.class,
                () -> agenda.reagendar(new Agendamento("Outra", inicio, inicio.plusHours(1)),
                                      inicio.plusHours(3), inicio.plusHours(4)));
        assertThrows(NullPointerException.class,
                () -> agenda.reagendar(null, inicio, inicio.plusHours(1)));
        assertEquals(snapshot, agenda.listar());
        assertFalse(agenda.disponivel(primeiro.inicio(), primeiro.fim()));
        assertFalse(agenda.disponivel(segundo.inicio(), segundo.fim()));
    }

    @Test void chamadasConcorrentesNaoDuplicamOMesmoHorario() throws Exception {
        AgendaService agenda = new AgendaService();
        var inicio = LocalDateTime.of(2026, 11, 3, 9, 0);
        var pool = java.util.concurrent.Executors.newFixedThreadPool(2);
        var start = new java.util.concurrent.CountDownLatch(1);
        try {
            var tasks = java.util.List.<java.util.concurrent.Callable<Boolean>>of(
                () -> { start.await(); try { agenda.agendar("A", inicio, inicio.plusHours(1)); return true; }
                        catch (IllegalStateException ignored) { return false; } },
                () -> { start.await(); try { agenda.agendar("B", inicio, inicio.plusHours(1)); return true; }
                        catch (IllegalStateException ignored) { return false; } }
            );
            var results = tasks.stream().map(pool::submit).toList();
            start.countDown();
            int accepted = 0;
            for (var result : results) if (result.get(5, java.util.concurrent.TimeUnit.SECONDS)) accepted++;
            assertEquals(1, accepted);
            assertEquals(1, agenda.listar().size());
        } finally {
            start.countDown();
            pool.shutdownNow();
            assertTrue(pool.awaitTermination(5, java.util.concurrent.TimeUnit.SECONDS));
        }
    }

}
