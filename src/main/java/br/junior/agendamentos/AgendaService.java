package br.junior.agendamentos;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public final class AgendaService {
    private final List<Agendamento> agendamentos = new ArrayList<>();
    public boolean disponivel(LocalDateTime inicio, LocalDateTime fim) {
        Agendamento tentativa = new Agendamento("consulta", inicio, fim);
        return agendamentos.stream().noneMatch(tentativa::conflita);
    }
    public Agendamento agendar(String cliente, LocalDateTime inicio, LocalDateTime fim) {
        Agendamento novo = new Agendamento(cliente, inicio, fim);
        if (agendamentos.stream().anyMatch(novo::conflita)) throw new IllegalStateException("horário já reservado");
        agendamentos.add(novo);
        return novo;
    }
    public List<Agendamento> listar() { return List.copyOf(agendamentos); }
}
