package br.junior.agendamentos;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Optional;

/** Intervalo semiaberto [inicio, fim), permitindo reservas consecutivas sem conflito. */
public record Intervalo(LocalDateTime inicio, LocalDateTime fim) implements Comparable<Intervalo> {
    public Intervalo {
        Objects.requireNonNull(inicio, "inicio");
        Objects.requireNonNull(fim, "fim");
        if (!fim.isAfter(inicio)) {
            throw new IllegalArgumentException("fim deve ser posterior ao inicio");
        }
    }

    public boolean conflita(Intervalo outro) {
        Objects.requireNonNull(outro, "outro");
        return inicio.isBefore(outro.fim) && outro.inicio.isBefore(fim);
    }

    public boolean contem(LocalDateTime instante) {
        Objects.requireNonNull(instante, "instante");
        return !instante.isBefore(inicio) && instante.isBefore(fim);
    }

    public boolean contem(Intervalo outro) {
        Objects.requireNonNull(outro, "outro");
        return !outro.inicio.isBefore(inicio) && !outro.fim.isAfter(fim);
    }

    public Optional<Intervalo> intersecao(Intervalo outro) {
        if (!conflita(outro)) {
            return Optional.empty();
        }
        LocalDateTime inicioComum = inicio.isAfter(outro.inicio) ? inicio : outro.inicio;
        LocalDateTime fimComum = fim.isBefore(outro.fim) ? fim : outro.fim;
        return Optional.of(new Intervalo(inicioComum, fimComum));
    }

    public Duration duracao() {
        return Duration.between(inicio, fim);
    }

    public Intervalo deslocar(Duration delta) {
        Objects.requireNonNull(delta, "delta");
        return new Intervalo(inicio.plus(delta), fim.plus(delta));
    }

    @Override
    public int compareTo(Intervalo outro) {
        int ordem = inicio.compareTo(outro.inicio);
        return ordem != 0 ? ordem : fim.compareTo(outro.fim);
    }
}
