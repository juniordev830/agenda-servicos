package br.junior.agendamentos;

import java.time.LocalDateTime;
import java.util.Objects;

public record Agendamento(String cliente, LocalDateTime inicio, LocalDateTime fim) {
    public Agendamento {
        cliente = Objects.requireNonNull(cliente, "cliente").trim();
        if (cliente.isEmpty()) throw new IllegalArgumentException("cliente obrigatório");
        Objects.requireNonNull(inicio, "inicio");
        Objects.requireNonNull(fim, "fim");
        if (!fim.isAfter(inicio)) throw new IllegalArgumentException("fim deve ser posterior ao início");
    }
    public boolean conflita(Agendamento outro) {
        return inicio.isBefore(outro.fim) && outro.inicio.isBefore(fim);
    }
}
