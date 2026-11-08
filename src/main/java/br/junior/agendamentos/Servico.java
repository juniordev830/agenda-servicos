package br.junior.agendamentos;

import java.time.Duration;
import java.util.Objects;

/** Serviço comercial com duração e preço definidos em centavos, sem arredondamento monetário. */
public record Servico(String codigo, String nome, Duration duracao, long precoCentavos) {
    public Servico {
        codigo = Objects.requireNonNull(codigo, "codigo").trim();
        nome = Objects.requireNonNull(nome, "nome").trim();
        duracao = Objects.requireNonNull(duracao, "duracao");
        if (!codigo.matches("[a-zA-Z0-9][a-zA-Z0-9_-]{0,39}")) {
            throw new IllegalArgumentException("codigo de servico invalido");
        }
        if (nome.isBlank() || nome.length() > 120) {
            throw new IllegalArgumentException("nome deve ter entre 1 e 120 caracteres");
        }
        if (duracao.isZero() || duracao.isNegative() || duracao.compareTo(Duration.ofHours(8)) > 0
                || duracao.toMinutes() * 60 != duracao.getSeconds()) {
            throw new IllegalArgumentException("duracao deve ter minutos inteiros entre 1 e 480");
        }
        if (precoCentavos < 0) {
            throw new IllegalArgumentException("preco nao pode ser negativo");
        }
    }

    public long minutos() {
        return duracao.toMinutes();
    }

    public Servico comPreco(long novoPrecoCentavos) {
        return new Servico(codigo, nome, duracao, novoPrecoCentavos);
    }

    public Servico comDuracao(Duration novaDuracao) {
        return new Servico(codigo, nome, novaDuracao, precoCentavos);
    }
}
