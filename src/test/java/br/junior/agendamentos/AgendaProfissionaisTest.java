package br.junior.agendamentos;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AgendaProfissionaisTest {
    private final LocalDate segunda = LocalDate.of(2026, 11, 9);
    private AgendaProfissionais agenda;
    private CatalogoServicos catalogo;

    @BeforeEach void configurar() {
        catalogo = new CatalogoServicos();
        catalogo.cadastrar(new Servico("consulta", "Consulta", Duration.ofMinutes(30), 5000));
        catalogo.cadastrar(new Servico("retorno", "Retorno", Duration.ofMinutes(60), 0));
        agenda = new AgendaProfissionais(catalogo);
        CalendarioTrabalho primeiro = new CalendarioTrabalho();
        primeiro.definirDiasUteis(LocalTime.of(9, 0), LocalTime.of(17, 0));
        CalendarioTrabalho segundo = new CalendarioTrabalho();
        segundo.definirDiasUteis(LocalTime.of(9, 0), LocalTime.of(17, 0));
        agenda.cadastrarProfissional("ana", primeiro);
        agenda.cadastrarProfissional("bia", segundo);
    }

    @Test void reservasSaoIndependentesPorProfissional() {
        var primeira = agenda.agendar("ana", "consulta", "Cliente 1", segunda.atTime(9, 0));
        var segundaReserva = agenda.agendar("bia", "consulta", "Cliente 2", segunda.atTime(9, 0));
        assertNotEquals(primeira.id(), segundaReserva.id());
        assertEquals(2, agenda.listarAtivas().size());
        assertFalse(agenda.disponivel("ana", primeira.horario()));
        assertFalse(agenda.disponivel("bia", segundaReserva.horario()));
        assertThrows(IllegalStateException.class, () -> agenda.agendar(
                "ana", "consulta", "Outro", segunda.atTime(9, 15)));
        assertEquals(2, agenda.resumo().confirmadas());
        assertEquals(10000, agenda.resumo().faturamentoPrevistoCentavos());
    }

    @Test void reagendamentoConservaIdEPrecoHistorico() {
        var original = agenda.agendar("ana", "consulta", "Maria", segunda.atTime(9, 0));
        catalogo.atualizar(catalogo.obter("consulta").comPreco(8000));
        var alterada = agenda.reagendar(original.id(), segunda.atTime(10, 0));
        assertEquals(original.id(), alterada.id());
        assertEquals(5000, alterada.precoCentavos());
        assertEquals(segunda.atTime(10, 0), agenda.obter(original.id()).horario().inicio());
        assertTrue(agenda.disponivel("ana", original.horario()));
        assertEquals(1, agenda.listarAtivas().size());
    }

    @Test void reagendamentoRejeitadoNaoAlteraReservaOriginal() {
        var primeira = agenda.agendar("ana", "consulta", "Joana", segunda.atTime(9, 0));
        var segundaReserva = agenda.agendar("ana", "consulta", "Paulo", segunda.atTime(10, 0));
        assertThrows(IllegalStateException.class, () -> agenda.reagendar(
                segundaReserva.id(), segunda.atTime(9, 0)));
        assertEquals(segunda.atTime(10, 0), agenda.obter(segundaReserva.id()).horario().inicio());
        assertThrows(IllegalArgumentException.class, () -> agenda.reagendar(
                primeira.id(), segunda.atTime(18, 0)));
        assertEquals(segunda.atTime(9, 0), agenda.obter(primeira.id()).horario().inicio());
    }

    @Test void cancelamentoPreservaHistoricoELiberaHorario() {
        var reserva = agenda.agendar("ana", "consulta", "Ana", segunda.atTime(9, 0));
        assertTrue(agenda.cancelar(reserva.id()));
        assertFalse(agenda.cancelar(reserva.id()));
        assertFalse(agenda.cancelar("nao-existe"));
        assertTrue(agenda.disponivel("ana", reserva.horario()));
        assertEquals(0, agenda.listarAtivas().size());
        assertEquals(1, agenda.listar().size());
        assertEquals(AgendaProfissionais.Estado.CANCELADA, agenda.obter(reserva.id()).estado());
        assertEquals(1, agenda.resumo().canceladas());
        assertThrows(IllegalStateException.class, () -> agenda.reagendar(
                reserva.id(), segunda.atTime(11, 0)));
    }

    @Test void consultasDeClienteDiaEPeriodo() {
        var maria = agenda.agendar("ana", "consulta", "Maria", segunda.atTime(9, 0));
        agenda.agendar("bia", "consulta", "maria", segunda.atTime(10, 0));
        agenda.agendar("ana", "retorno", "Pedro", segunda.plusDays(1).atTime(9, 0));
        assertEquals(2, agenda.porCliente("MARIA").size());
        assertEquals(2, agenda.porProfissional("ana").size());
        assertEquals(2, agenda.noDia(segunda).size());
        assertEquals(1, agenda.noPeriodo(new Intervalo(
                segunda.atTime(9, 0), segunda.atTime(9, 30))).size());
        assertEquals(maria.id(), agenda.noPeriodo(new Intervalo(
                segunda.atTime(9, 0), segunda.atTime(9, 30))).get(0).id());
        assertThrows(UnsupportedOperationException.class, () -> agenda.listar().clear());
    }

    @Test void horariosLivresConsideramPausasFeriadosEBloqueios() {
        agenda.agendar("ana", "consulta", "Maria", segunda.atTime(9, 0));
        var livres = agenda.horariosLivres("ana", "consulta", segunda, Duration.ofMinutes(30));
        assertEquals(15, livres.size());
        assertEquals(segunda.atTime(9, 30), livres.get(0).inicio());
        agenda.calendario("ana").bloquear(new Intervalo(segunda.atTime(10, 0),
                segunda.atTime(11, 0)));
        assertEquals(13, agenda.horariosLivres("ana", "consulta", segunda,
                Duration.ofMinutes(30)).size());
        agenda.calendario("ana").adicionarFeriado(segunda);
        assertTrue(agenda.horariosLivres("ana", "consulta", segunda,
                Duration.ofMinutes(30)).isEmpty());
        assertEquals(0, agenda.taxaOcupacao("ana", segunda));
    }

    @Test void proximoHorarioRespeitaLimitesEBuscaMultiDia() {
        agenda.calendario("ana").adicionarFeriado(segunda);
        var proximo = agenda.proximoHorario("ana", "consulta", segunda, 3,
                Duration.ofMinutes(30)).orElseThrow();
        assertEquals(segunda.plusDays(1).atTime(9, 0), proximo.inicio());
        assertTrue(agenda.proximoHorario("ana", "consulta", segunda, 1,
                Duration.ofMinutes(30)).isEmpty());
        assertThrows(IllegalArgumentException.class, () -> agenda.proximoHorario(
                "ana", "consulta", segunda, 367, Duration.ofMinutes(30)));
        assertThrows(IllegalArgumentException.class, () -> agenda.proximoHorario(
                "ana", "consulta", segunda, 0, Duration.ofMinutes(30)));
    }

    @Test void servicoInativoNaoPodeSerAgendado() {
        catalogo.desativar("consulta");
        assertThrows(IllegalArgumentException.class, () -> agenda.agendar(
                "ana", "consulta", "Cliente", segunda.atTime(9, 0)));
        assertThrows(IllegalArgumentException.class, () -> agenda.horariosLivres(
                "ana", "consulta", segunda, Duration.ofMinutes(30)));
        assertThrows(IllegalArgumentException.class, () -> agenda.agendar(
                "desconhecido", "retorno", "Cliente", segunda.atTime(9, 0)));
        assertThrows(IllegalArgumentException.class, () -> agenda.cadastrarProfissional(
                "ana", new CalendarioTrabalho()));
        assertThrows(IllegalArgumentException.class, () -> agenda.obter("desconhecido"));
    }

    @Test void relatorioCalculaMinutosReceitaETaxa() {
        agenda.agendar("ana", "consulta", "Maria", segunda.atTime(9, 0));
        agenda.agendar("ana", "retorno", "Pedro", segunda.atTime(10, 0));
        var cancelada = agenda.agendar("bia", "consulta", "Carlos", segunda.atTime(11, 0));
        agenda.cancelar(cancelada.id());
        assertEquals(90, agenda.resumo().minutosReservados());
        assertEquals(5000, agenda.resumo().faturamentoPrevistoCentavos());
        assertEquals(2, agenda.resumo().confirmadas());
        assertEquals(1, agenda.resumo().canceladas());
        assertEquals(90, agenda.minutosOcupados("ana", segunda));
        assertEquals(90.0 / 480, agenda.taxaOcupacao("ana", segunda), 0.00001);
    }
}
