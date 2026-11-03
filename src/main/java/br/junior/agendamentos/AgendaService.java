package br.junior.agendamentos;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class AgendaService {
    private final List<Agendamento> agendamentos = new ArrayList<>();
    public synchronized boolean disponivel(LocalDateTime inicio, LocalDateTime fim) {
        Agendamento tentativa = new Agendamento("consulta", inicio, fim);
        return agendamentos.stream().noneMatch(tentativa::conflita);
    }
    public synchronized Agendamento agendar(String cliente, LocalDateTime inicio, LocalDateTime fim) {
        Agendamento novo = new Agendamento(cliente, inicio, fim);
        if (agendamentos.stream().anyMatch(novo::conflita)) throw new IllegalStateException("horário já reservado");
        agendamentos.add(novo);
        return novo;
    }
    /**
     * Reschedules an existing appointment without first cancelling its old slot.
     * When validation or conflict checks fail, the original booking remains intact.
     * All reads and writes share the service monitor.
     */
    public synchronized Agendamento reagendar(
            Agendamento atual, LocalDateTime novoInicio, LocalDateTime novoFim) {
        Objects.requireNonNull(atual, "agendamento");
        int index = agendamentos.indexOf(atual);
        if (index < 0) throw new IllegalArgumentException("agendamento não encontrado");

        Agendamento atualizado = new Agendamento(atual.cliente(), novoInicio, novoFim);
        for (int i = 0; i < agendamentos.size(); i++) {
            if (i != index && atualizado.conflita(agendamentos.get(i))) {
                throw new IllegalStateException("horário já reservado");
            }
        }
        agendamentos.set(index, atualizado);
        return atualizado;
    }

    public synchronized boolean cancelar(Agendamento agendamento) {
        Objects.requireNonNull(agendamento, "agendamento");
        return agendamentos.remove(agendamento);
    }

    public synchronized List<Agendamento> listar() { return List.copyOf(agendamentos); }
}
