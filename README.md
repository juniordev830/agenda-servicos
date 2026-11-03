# Agenda Serviços

Núcleo de agendamento em **Java 17**, com consulta de disponibilidade, prevenção de sobreposição, cancelamento e remarcação segura.

[![CI](https://github.com/juniordev830/agenda-servicos/actions/workflows/ci.yml/badge.svg)](https://github.com/juniordev830/agenda-servicos/actions)

## Regras implementadas

| Caso | Resultado |
| --- | --- |
| Intervalos sobrepostos | Novo agendamento rejeitado |
| Um horário termina quando outro começa | Ambos permitidos |
| Fim igual ou anterior ao início | Entrada rejeitada |
| Cliente vazio ou data ausente | Entrada rejeitada |
| Cancelamento de registro existente | Remove somente o registro e libera o intervalo |
| Cancelamento repetido ou inexistente | Retorna false e preserva os demais registros |
| Remarcação de horário | Substitui a reserva anterior somente após validar o novo intervalo e os conflitos |
| Reservas concorrentes | Métodos públicos sincronizados impedem dupla reserva dentro do mesmo processo |

Os intervalos seguem a convenção `[início, fim)`. Cancelamento e remarcação localizam a reserva pela igualdade do record imutável `Agendamento` (cliente, início e fim), sem ID persistente. Após remarcar, utilize o objeto retornado nas próximas operações.

## Exemplo de uso

```java
var agenda = new AgendaService();
var inicio = LocalDateTime.of(2026, 11, 3, 9, 0);
var reserva = agenda.agendar("Cliente fictício", inicio, inicio.plusHours(1));
System.out.println(agenda.disponivel(inicio, inicio.plusHours(1))); // false
var atualizada = agenda.reagendar(reserva, inicio.plusHours(2), inicio.plusHours(3));
agenda.cancelar(atualizada);
System.out.println(agenda.disponivel(inicio, inicio.plusHours(1))); // true
```

Código: [serviço](src/main/java/br/junior/agendamentos/AgendaService.java) · [modelo](src/main/java/br/junior/agendamentos/Agendamento.java) · [testes](src/test/java/br/junior/agendamentos/AgendaServiceTest.java).

## Verificação

Requisitos: JDK 17 e Maven instalado. Na raiz:

```sh
mvn -B test
```

O CI executa testes de conflitos, horários adjacentes, cancelamento, remarcações válidas e inválidas, concorrência de duas reservas e preservação dos demais agendamentos. As consultas devolvem snapshots não modificáveis.

## Estado e limites

Biblioteca de domínio testável, sem API, interface, banco ou CLI. A agenda representa um único profissional/recurso e usa `LocalDateTime`, sem conversão de fuso horário. Os dados ficam em memória. Os métodos públicos são sincronizados para concorrência entre threads da **mesma instância**; isso não equivale a transações distribuídas ou bloqueios entre réplicas. Ainda faltam catálogo de serviços/durações, identidade persistente dos clientes e banco de dados.

Os exemplos usam dados fictícios. Não versionar credenciais ou dados pessoais. Licença ainda não definida.
