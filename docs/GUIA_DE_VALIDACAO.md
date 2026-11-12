# Roteiro de manutenção — Agenda Serviços

## O que o código faz

Núcleo Java 17 de agendamentos, com prevenção de intervalos sobrepostos e cancelamento.

## Arquivos de referência

`src/main/java/br/junior/agendamentos/AgendaService.java`, `Agendamento.java` e `src/test/java/br/junior/agendamentos/AgendaServiceTest.java`.

## Executar verificações

`mvn -B test` com JDK 17 e Maven.

Use ambiente local e dados fictícios. O CI complementa, mas não substitui a inspeção do comportamento e das entradas de fronteira.

## Testes que não podem regredir

Conferir intervalos adjacentes, cancelamento repetido, início/fim invertidos, cliente em branco e colisão no mesmo recurso.

## Próxima melhoria útil

Projetar persistência e regras de fuso antes de integrar agenda real ou calendário online.

### Antes de abrir PR

- [ ] Teste ou exemplo reproduzível para a alteração.
- [ ] README atualizado se o comando ou escopo mudou.
- [ ] Dados privados e segredos fora do controle de versão.
- [ ] Revisão em branch, sem merge automático.
