# Agenda Serviços

Núcleo de agendamento em **Java 17**, com consulta de disponibilidade, prevenção de sobreposição e cancelamento.

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

Os intervalos seguem a convenção `[início, fim)`. O cancelamento usa a igualdade do record imutável `Agendamento` (cliente, início e fim), sem ID persistente.

## Exemplo de uso

```java
var agenda = new AgendaService();
var inicio = LocalDateTime.of(2026, 11, 3, 9, 0);
var reserva = agenda.agendar("Cliente fictício", inicio, inicio.plusHours(1));
System.out.println(agenda.disponivel(inicio, inicio.plusHours(1))); // false
agenda.cancelar(reserva);
System.out.println(agenda.disponivel(inicio, inicio.plusHours(1))); // true
```

Código: [serviço](src/main/java/br/junior/agendamentos/AgendaService.java) · [modelo](src/main/java/br/junior/agendamentos/Agendamento.java) · [testes](src/test/java/br/junior/agendamentos/AgendaServiceTest.java).

## Consulta de agenda por dia

`listarPorDia(LocalDate)` devolve um snapshot imutável ordenado por início.
Inclui reservas que atravessam a meia-noite, respeitando intervalos `[início, fim)`.

```java
var reservas = agenda.listarPorDia(LocalDate.of(2026, 11, 3));
```

## Verificação

Requisitos: JDK 17 e Maven instalado. Na raiz:

```sh
mvn -B test
```

O CI executa testes de conflitos, horários adjacentes, cancelamento, preservação dos demais agendamentos e entradas inválidas. As consultas devolvem snapshots não modificáveis.

## Estado e limites

Biblioteca de domínio testável, sem API, interface, banco ou CLI. A agenda representa um único profissional/recurso e usa `LocalDateTime`, sem conversão de fuso horário. Os dados ficam em memória e o serviço é destinado a uso sequencial em um único processo. Ainda faltam catálogo de serviços/durações, identidade dos clientes, persistência e controle de concorrência.

Os exemplos usam dados fictícios. Não versionar credenciais ou dados pessoais. Licença ainda não definida.
