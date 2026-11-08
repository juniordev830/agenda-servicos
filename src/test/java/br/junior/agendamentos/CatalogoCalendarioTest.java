package br.junior.agendamentos;

import static org.junit.jupiter.api.Assertions.*;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.Test;

class CatalogoCalendarioTest {
    private static final LocalDate SEGUNDA = LocalDate.of(2026, 11, 9);

    @Test void validaServicoEPrecoSemPontoFlutuante() {
        Servico corte = new Servico("corte", "Corte", Duration.ofMinutes(30), 2500);
        assertEquals(30, corte.minutos());
        assertEquals(3000, corte.comPreco(3000).precoCentavos());
        assertEquals(45, corte.comDuracao(Duration.ofMinutes(45)).minutos());
        assertEquals(2500, corte.precoCentavos());
        assertThrows(IllegalArgumentException.class,
                () -> new Servico("codigo invalido", "Corte", Duration.ofMinutes(30), 100));
        assertThrows(IllegalArgumentException.class,
                () -> new Servico("c", "  ", Duration.ofMinutes(30), 100));
        assertThrows(IllegalArgumentException.class,
                () -> new Servico("c", "Corte", Duration.ZERO, 100));
        assertThrows(IllegalArgumentException.class,
                () -> new Servico("c", "Corte", Duration.ofHours(9), 100));
        assertThrows(IllegalArgumentException.class,
                () -> new Servico("c", "Corte", Duration.ofSeconds(90), 100));
        assertThrows(IllegalArgumentException.class,
                () -> new Servico("c", "Corte", Duration.ofMinutes(30), -1));
    }

    @Test void catalogoGerenciaAtivacaoBuscaEPrecos() {
        CatalogoServicos catalogo = new CatalogoServicos();
        Servico corte = new Servico("corte", "Corte simples", Duration.ofMinutes(30), 2000);
        Servico barba = new Servico("barba", "Barba", Duration.ofMinutes(20), 1500);
        catalogo.cadastrar(corte);
        catalogo.cadastrar(barba);
        assertEquals(2, catalogo.quantidadeAtivos());
        assertEquals(3500, catalogo.somarPrecos(List.of("corte", "barba")));
        assertEquals(1, catalogo.buscar("SIMPLES").size());
        assertEquals(1, catalogo.buscar("barba").size());
        assertThrows(IllegalArgumentException.class, () -> catalogo.cadastrar(corte));
        catalogo.desativar("barba");
        assertEquals(1, catalogo.quantidadeAtivos());
        assertFalse(catalogo.ativo("barba"));
        assertThrows(IllegalArgumentException.class, () -> catalogo.somarPrecos(List.of("barba")));
        catalogo.ativar("barba");
        catalogo.atualizar(barba.comPreco(1800));
        assertEquals(3800, catalogo.somarPrecos(List.of("corte", "barba")));
        assertThrows(IllegalArgumentException.class, () -> catalogo.atualizar(
                new Servico("inexistente", "Novo", Duration.ofMinutes(10), 0)));
        assertThrows(UnsupportedOperationException.class, () -> catalogo.listar().clear());
    }

    @Test void intervalosSaoSemiabertosEInterseccaoPrecisa() {
        LocalDateTime nove = SEGUNDA.atTime(9, 0);
        Intervalo primeiro = new Intervalo(nove, nove.plusHours(1));
        Intervalo segundo = new Intervalo(nove.plusHours(1), nove.plusHours(2));
        Intervalo sobreposto = new Intervalo(nove.plusMinutes(45), nove.plusMinutes(75));
        assertFalse(primeiro.conflita(segundo));
        assertTrue(primeiro.conflita(sobreposto));
        assertTrue(primeiro.contem(nove));
        assertFalse(primeiro.contem(nove.plusHours(1)));
        assertTrue(primeiro.contem(new Intervalo(nove.plusMinutes(5), nove.plusMinutes(30))));
        assertEquals(Duration.ofHours(1), primeiro.duracao());
        assertEquals(new Intervalo(nove.plusMinutes(45), nove.plusHours(1)),
                primeiro.intersecao(sobreposto).orElseThrow());
        assertTrue(primeiro.intersecao(segundo).isEmpty());
        assertEquals(segundo, primeiro.deslocar(Duration.ofHours(1)));
        assertTrue(primeiro.compareTo(segundo) < 0);
        assertThrows(IllegalArgumentException.class, () -> new Intervalo(nove, nove));
    }

    @Test void calendarioRejeitaExpedientesSobrepostosEPermitePausa() {
        CalendarioTrabalho calendario = new CalendarioTrabalho();
        calendario.definirExpediente(DayOfWeek.MONDAY, List.of(
                new CalendarioTrabalho.Expediente(LocalTime.of(13, 0), LocalTime.of(18, 0)),
                new CalendarioTrabalho.Expediente(LocalTime.of(8, 0), LocalTime.of(12, 0))));
        assertEquals(2, calendario.expedientes(DayOfWeek.MONDAY).size());
        assertEquals(540, calendario.minutosDeExpediente(SEGUNDA));
        assertTrue(calendario.permite(intervalo(9, 0, 10, 0)));
        assertFalse(calendario.permite(intervalo(11, 30, 13, 30)));
        assertFalse(calendario.permite(intervalo(7, 0, 8, 0)));
        assertThrows(IllegalArgumentException.class, () -> calendario.definirExpediente(
                DayOfWeek.MONDAY, List.of(
                        new CalendarioTrabalho.Expediente(LocalTime.of(9, 0), LocalTime.of(12, 0)),
                        new CalendarioTrabalho.Expediente(LocalTime.of(11, 0), LocalTime.of(14, 0)))));
        assertEquals(2, calendario.expedientes(DayOfWeek.MONDAY).size());
    }

    @Test void bloqueiosEFeriadosNaoVazamEstado() {
        CalendarioTrabalho calendario = new CalendarioTrabalho();
        calendario.definirDiasUteis(LocalTime.of(8, 0), LocalTime.of(17, 0));
        Intervalo bloqueio = intervalo(9, 0, 10, 0);
        calendario.bloquear(bloqueio);
        assertFalse(calendario.permite(intervalo(9, 30, 10, 30)));
        assertTrue(calendario.permite(intervalo(10, 0, 11, 0)));
        assertThrows(IllegalArgumentException.class, () -> calendario.bloquear(bloqueio));
        assertThrows(UnsupportedOperationException.class, () -> calendario.bloqueios().clear());
        assertTrue(calendario.desbloquear(bloqueio));
        assertFalse(calendario.desbloquear(bloqueio));
        assertTrue(calendario.permite(bloqueio));
        calendario.adicionarFeriado(SEGUNDA);
        assertTrue(calendario.feriado(SEGUNDA));
        assertEquals(0, calendario.minutosDeExpediente(SEGUNDA));
        assertTrue(calendario.gerarCandidatos(SEGUNDA, Duration.ofMinutes(30),
                Duration.ofMinutes(15)).isEmpty());
        assertTrue(calendario.removerFeriado(SEGUNDA));
        assertFalse(calendario.feriado(SEGUNDA));
    }

    @Test void slotsRespeitamDuracaoPassoEBloqueio() {
        CalendarioTrabalho calendario = new CalendarioTrabalho();
        calendario.definirExpediente(DayOfWeek.MONDAY, List.of(
                new CalendarioTrabalho.Expediente(LocalTime.of(9, 0), LocalTime.of(11, 0))));
        assertEquals(3, calendario.gerarCandidatos(SEGUNDA, Duration.ofHours(1),
                Duration.ofMinutes(30)).size());
        calendario.bloquear(intervalo(9, 45, 10, 15));
        assertTrue(calendario.gerarCandidatos(SEGUNDA, Duration.ofHours(1),
                Duration.ofMinutes(30)).isEmpty());
        assertThrows(IllegalArgumentException.class, () -> calendario.gerarCandidatos(
                SEGUNDA, Duration.ZERO, Duration.ofMinutes(30)));
        assertThrows(IllegalArgumentException.class, () -> calendario.gerarCandidatos(
                SEGUNDA, Duration.ofMinutes(30), Duration.ofSeconds(30)));
        assertTrue(calendario.gerarCandidatos(SEGUNDA.plusDays(1), Duration.ofMinutes(30),
                Duration.ofMinutes(30)).isEmpty());
    }

    private static Intervalo intervalo(int horaInicio, int minutoInicio, int horaFim, int minutoFim) {
        return new Intervalo(SEGUNDA.atTime(horaInicio, minutoInicio),
                SEGUNDA.atTime(horaFim, minutoFim));
    }
}
