# Agenda Serviços

Biblioteca de domínio em **Java 17**, sem dependências de produção, para agendamentos de serviços. O módulo original `AgendaService` continua disponível e mantém a mesma API; a expansão adiciona recursos independentes para catálogos, múltiplos profissionais e relatórios.

[![CI](https://github.com/juniordev830/agenda-servicos/actions/workflows/ci.yml/badge.svg)](https://github.com/juniordev830/agenda-servicos/actions)

## Funcionalidades

- **Catálogo:** cadastro e atualização de serviços, duração validada em minutos, preço inteiro em centavos, pesquisa, ativação e desativação.
- **Calendário de trabalho:** expediente por dia da semana, múltiplos turnos com pausa, feriados, bloqueios excepcionais e geração de candidatos para reservas.
- **Agenda multiprofissional:** reservas com IDs estáveis, preços históricos, isolamento entre profissionais, conflito de horários, cancelamento auditável e reagendamento atômico.
- **Disponibilidade:** intervalos semiabertos `[início, fim)`, slots configuráveis e busca do primeiro horário em até 366 dias.
- **Consultas e indicadores:** reservas por profissional, cliente, período e dia; ocupação, minutos reservados e receita prevista.
- **Exportação:** CSV com aspas e mitigação de injeção de fórmulas em planilhas; iCalendar com escape de caracteres e exclusão de cancelamentos.

## Exemplo

```java
var catalogo = new CatalogoServicos();
catalogo.cadastrar(new Servico("consulta", "Consulta", Duration.ofMinutes(30), 5000));

var horario = new CalendarioTrabalho();
horario.definirDiasUteis(LocalTime.of(9, 0), LocalTime.of(17, 0));

var agenda = new AgendaProfissionais(catalogo);
agenda.cadastrarProfissional("profissional-1", horario);
var reserva = agenda.agendar("profissional-1", "consulta", "Cliente fictício",
    LocalDateTime.of(2026, 11, 9, 9, 0));

System.out.println(agenda.resumo().faturamentoPrevistoCentavos());
System.out.println(RelatoriosAgenda.csv(agenda.listar()));
System.out.println(RelatoriosAgenda.ics(agenda.listar(), "Agenda"));
```

Imports necessários: `br.junior.agendamentos.*`, `java.time.*`.

## Testes e CI

```sh
mvn -B test
```

O projeto usa JUnit 5 e GitHub Actions. Os testes cobrem validações, conflitos, horários adjacentes, concorrência sequencial por operação sincronizada, calendários, cancelamentos, reagendamentos, filtros e exportações. A API original permanece coberta por seus testes anteriores.

## Limites e decisões

1. A aplicação **não** persiste reservas nem expõe endpoints HTTP. Os dados existem apenas durante a execução do processo.
2. Os calendários e reservas usam `LocalDateTime`; o sistema consumidor deve definir e documentar seu fuso horário. O arquivo ICS usa horários locais, sem declarar UTC.
3. O controle de concorrência é sincronizado em cada instância de `AgendaProfissionais`; não substitui transações de banco de dados entre processos.
4. Alterar um serviço não modifica preço ou duração de reservas já registradas. Bloqueios adicionados depois de reservas existentes não cancelam automaticamente esses agendamentos.
5. O percentual de ocupação considera a duração do expediente configurado, sem descontar pausas extraordinárias. O relatório financeiro representa **receita prevista**, não pagamentos recebidos.
6. O CSV exportado é para planilhas; não é um formato de backup/importação. Evite exportar dados pessoais sem base legal e controle de acesso.

O módulo original de agendamento simples continua em `AgendaService` e `Agendamento`, sem mudança de comportamento. Todos os exemplos usam dados fictícios.
