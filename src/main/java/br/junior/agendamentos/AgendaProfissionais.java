package br.junior.agendamentos;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Predicate;

/**
 * Agenda de vários profissionais com reservas identificadas, histórico de cancelamentos
 * e operações atômicas em memória. Não altera o serviço simples AgendaService existente.
 */
public final class AgendaProfissionais {
    public enum Estado { CONFIRMADA, CANCELADA }

    public record Reserva(
            String id, String profissional, String codigoServico, String cliente,
            Intervalo horario, long precoCentavos, Estado estado) {
        public Reserva {
            validarTexto(id, "id");
            validarTexto(profissional, "profissional");
            validarTexto(codigoServico, "codigoServico");
            validarTexto(cliente, "cliente");
            Objects.requireNonNull(horario, "horario");
            Objects.requireNonNull(estado, "estado");
            if (precoCentavos < 0) {
                throw new IllegalArgumentException("preco negativo");
            }
        }

        public Reserva comHorario(Intervalo novoHorario) {
            return new Reserva(id, profissional, codigoServico, cliente,
                    novoHorario, precoCentavos, estado);
        }

        public Reserva cancelar() {
            return new Reserva(id, profissional, codigoServico, cliente,
                    horario, precoCentavos, Estado.CANCELADA);
        }

        public boolean ativa() {
            return estado == Estado.CONFIRMADA;
        }
    }

    public record Resumo(long confirmadas, long canceladas, long minutosReservados,
                         long faturamentoPrevistoCentavos) {}

    private final CatalogoServicos catalogo;
    private final Map<String, CalendarioTrabalho> profissionais = new LinkedHashMap<>();
    private final Map<String, Reserva> reservas = new LinkedHashMap<>();
    private final AtomicLong sequencia = new AtomicLong();

    public AgendaProfissionais(CatalogoServicos catalogo) {
        this.catalogo = Objects.requireNonNull(catalogo, "catalogo");
    }

    public synchronized void cadastrarProfissional(String identificador, CalendarioTrabalho calendario) {
        validarTexto(identificador, "profissional");
        Objects.requireNonNull(calendario, "calendario");
        if (profissionais.containsKey(identificador)) {
            throw new IllegalArgumentException("profissional ja cadastrado");
        }
        profissionais.put(identificador, calendario);
    }

    public synchronized List<String> profissionais() {
        return List.copyOf(profissionais.keySet());
    }

    public synchronized CalendarioTrabalho calendario(String profissional) {
        CalendarioTrabalho calendario = profissionais.get(Objects.requireNonNull(profissional));
        if (calendario == null) {
            throw new IllegalArgumentException("profissional desconhecido");
        }
        return calendario;
    }

    public synchronized Reserva agendar(String profissional, String codigoServico, String cliente,
                                        LocalDateTime inicio) {
        validarTexto(cliente, "cliente");
        Objects.requireNonNull(inicio, "inicio");
        Servico servico = catalogo.obter(codigoServico);
        if (!catalogo.ativo(codigoServico)) {
            throw new IllegalArgumentException("servico inativo");
        }
        Intervalo intervalo = new Intervalo(inicio, inicio.plus(servico.duracao()));
        exigirDisponivel(profissional, intervalo, null);
        String id = "RES-" + sequencia.incrementAndGet();
        Reserva reserva = new Reserva(id, profissional, codigoServico, cliente.strip(),
                intervalo, servico.precoCentavos(), Estado.CONFIRMADA);
        reservas.put(id, reserva);
        return reserva;
    }

    public synchronized boolean disponivel(String profissional, Intervalo intervalo) {
        Objects.requireNonNull(intervalo, "intervalo");
        if (!calendario(profissional).permite(intervalo)) {
            return false;
        }
        return reservas.values().stream().noneMatch(r -> r.ativa()
                && r.profissional().equals(profissional) && r.horario().conflita(intervalo));
    }

    public synchronized Reserva reagendar(String id, LocalDateTime novoInicio) {
        Objects.requireNonNull(novoInicio, "novoInicio");
        Reserva anterior = exigirAtiva(id);
        Intervalo intervalo = new Intervalo(novoInicio,
                novoInicio.plus(anterior.horario().duracao()));
        exigirDisponivel(anterior.profissional(), intervalo, id);
        Reserva atualizada = anterior.comHorario(intervalo);
        reservas.put(id, atualizada);
        return atualizada;
    }

    public synchronized boolean cancelar(String id) {
        Reserva atual = reservas.get(Objects.requireNonNull(id, "id"));
        if (atual == null || !atual.ativa()) {
            return false;
        }
        reservas.put(id, atual.cancelar());
        return true;
    }

    public synchronized Reserva obter(String id) {
        Reserva reserva = reservas.get(Objects.requireNonNull(id, "id"));
        if (reserva == null) {
            throw new IllegalArgumentException("reserva inexistente");
        }
        return reserva;
    }

    public synchronized List<Reserva> listar() {
        return List.copyOf(reservas.values());
    }

    public synchronized List<Reserva> listarAtivas() {
        return filtrar(Reserva::ativa);
    }

    public synchronized List<Reserva> porProfissional(String profissional) {
        calendario(profissional);
        return filtrar(r -> r.profissional().equals(profissional));
    }

    public synchronized List<Reserva> porCliente(String cliente) {
        validarTexto(cliente, "cliente");
        return filtrar(r -> r.cliente().equalsIgnoreCase(cliente.strip()));
    }

    public synchronized List<Reserva> noPeriodo(Intervalo periodo) {
        Objects.requireNonNull(periodo, "periodo");
        return filtrar(r -> r.horario().conflita(periodo));
    }

    public synchronized List<Reserva> noDia(LocalDate dia) {
        Objects.requireNonNull(dia, "dia");
        return filtrar(r -> r.horario().inicio().toLocalDate().equals(dia));
    }

    public synchronized List<Intervalo> horariosLivres(String profissional, String codigoServico,
                                                        LocalDate dia, Duration passo) {
        Servico servico = catalogo.obter(codigoServico);
        if (!catalogo.ativo(codigoServico)) {
            throw new IllegalArgumentException("servico inativo");
        }
        return calendario(profissional).gerarCandidatos(dia, servico.duracao(), passo)
                .stream().filter(i -> disponivel(profissional, i)).toList();
    }

    public synchronized Optional<Intervalo> proximoHorario(String profissional, String codigoServico,
                                                            LocalDate primeiroDia, int dias,
                                                            Duration passo) {
        Objects.requireNonNull(primeiroDia, "primeiroDia");
        if (dias < 1 || dias > 366) {
            throw new IllegalArgumentException("dias fora do limite de busca");
        }
        for (int deslocamento = 0; deslocamento < dias; deslocamento++) {
            List<Intervalo> livres = horariosLivres(profissional, codigoServico,
                    primeiroDia.plusDays(deslocamento), passo);
            if (!livres.isEmpty()) {
                return Optional.of(livres.get(0));
            }
        }
        return Optional.empty();
    }

    public synchronized Resumo resumo() {
        long confirmadas = 0;
        long canceladas = 0;
        long minutos = 0;
        long receita = 0;
        for (Reserva reserva : reservas.values()) {
            if (reserva.ativa()) {
                confirmadas++;
                minutos = Math.addExact(minutos, reserva.horario().duracao().toMinutes());
                receita = Math.addExact(receita, reserva.precoCentavos());
            } else {
                canceladas++;
            }
        }
        return new Resumo(confirmadas, canceladas, minutos, receita);
    }

    public synchronized long minutosOcupados(String profissional, LocalDate dia) {
        calendario(profissional);
        Objects.requireNonNull(dia, "dia");
        return reservas.values().stream()
                .filter(r -> r.ativa() && r.profissional().equals(profissional))
                .filter(r -> r.horario().inicio().toLocalDate().equals(dia))
                .mapToLong(r -> r.horario().duracao().toMinutes()).sum();
    }

    public synchronized double taxaOcupacao(String profissional, LocalDate dia) {
        long capacidade = calendario(profissional).minutosDeExpediente(dia);
        if (capacidade == 0) {
            return 0.0;
        }
        return Math.min(1.0, (double) minutosOcupados(profissional, dia) / capacidade);
    }

    private List<Reserva> filtrar(Predicate<Reserva> predicado) {
        List<Reserva> resultado = new ArrayList<>();
        for (Reserva reserva : reservas.values()) {
            if (predicado.test(reserva)) {
                resultado.add(reserva);
            }
        }
        resultado.sort(Comparator.comparing((Reserva r) -> r.horario().inicio())
                .thenComparing(Reserva::id));
        return List.copyOf(resultado);
    }

    private Reserva exigirAtiva(String id) {
        Reserva reserva = obter(id);
        if (!reserva.ativa()) {
            throw new IllegalStateException("reserva cancelada");
        }
        return reserva;
    }

    private void exigirDisponivel(String profissional, Intervalo intervalo, String ignorarId) {
        if (!calendario(profissional).permite(intervalo)) {
            throw new IllegalArgumentException("fora do expediente ou bloqueado");
        }
        boolean conflito = reservas.values().stream()
                .anyMatch(r -> r.ativa() && r.profissional().equals(profissional)
                        && !r.id().equals(ignorarId) && r.horario().conflita(intervalo));
        if (conflito) {
            throw new IllegalStateException("horario ocupado");
        }
    }

    private static void validarTexto(String texto, String campo) {
        Objects.requireNonNull(texto, campo);
        if (texto.isBlank() || texto.strip().length() > 160) {
            throw new IllegalArgumentException(campo + " deve ter entre 1 e 160 caracteres");
        }
    }
}
