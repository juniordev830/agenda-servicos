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
    public synchronized boolean cancelar(Agendamento agendamento) {
        Objects.requireNonNull(agendamento, "agendamento");
        return agendamentos.remove(agendamento);
    }

    public synchronized List<Agendamento> listar() { return List.copyOf(agendamentos); }
}
