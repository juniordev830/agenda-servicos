package br.junior.agendamentos;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** Catálogo em memória que mantém o estado ativo separado dos dados imutáveis do serviço. */
public final class CatalogoServicos {
    private final Map<String, Servico> servicos = new LinkedHashMap<>();
    private final Set<String> inativos = new LinkedHashSet<>();

    public synchronized void cadastrar(Servico servico) {
        Objects.requireNonNull(servico, "servico");
        if (servicos.containsKey(servico.codigo())) {
            throw new IllegalArgumentException("codigo de servico ja cadastrado");
        }
        servicos.put(servico.codigo(), servico);
    }

    public synchronized void atualizar(Servico servico) {
        Objects.requireNonNull(servico, "servico");
        if (!servicos.containsKey(servico.codigo())) {
            throw new IllegalArgumentException("servico inexistente");
        }
        servicos.put(servico.codigo(), servico);
    }

    public synchronized Servico obter(String codigo) {
        Servico servico = servicos.get(Objects.requireNonNull(codigo, "codigo"));
        if (servico == null) {
            throw new IllegalArgumentException("servico inexistente: " + codigo);
        }
        return servico;
    }

    public synchronized boolean ativo(String codigo) {
        return servicos.containsKey(codigo) && !inativos.contains(codigo);
    }

    public synchronized void desativar(String codigo) {
        obter(codigo);
        inativos.add(codigo);
    }

    public synchronized void ativar(String codigo) {
        obter(codigo);
        inativos.remove(codigo);
    }

    public synchronized List<Servico> listar() {
        return List.copyOf(servicos.values());
    }

    public synchronized List<Servico> listarAtivos() {
        List<Servico> resultado = new ArrayList<>();
        for (Servico servico : servicos.values()) {
            if (ativo(servico.codigo())) {
                resultado.add(servico);
            }
        }
        return List.copyOf(resultado);
    }

    public synchronized List<Servico> buscar(String termo) {
        String consulta = Objects.requireNonNull(termo, "termo").strip().toLowerCase(Locale.ROOT);
        return servicos.values().stream()
                .filter(s -> s.codigo().toLowerCase(Locale.ROOT).contains(consulta)
                        || s.nome().toLowerCase(Locale.ROOT).contains(consulta))
                .sorted(Comparator.comparing(Servico::nome).thenComparing(Servico::codigo))
                .toList();
    }

    public synchronized int quantidadeAtivos() {
        return servicos.size() - inativos.size();
    }

    public synchronized long somarPrecos(List<String> codigos) {
        Objects.requireNonNull(codigos, "codigos");
        long total = 0;
        for (String codigo : codigos) {
            if (!ativo(codigo)) {
                throw new IllegalArgumentException("servico inativo ou inexistente: " + codigo);
            }
            total = Math.addExact(total, obter(codigo).precoCentavos());
        }
        return total;
    }
}
