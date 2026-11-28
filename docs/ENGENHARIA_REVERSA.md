# Engenharia reversa e estudo — Agenda Serviços

## Objetivo e fluxo

Agendamento de serviços e disponibilidade de profissionais. Intervalo, CalendarioTrabalho e CatalogoServicos sustentam as verificações de AgendaProfissionais. Compare com a interface original de AgendaService.

**Tecnologias identificadas** · Java.

**Revisão de referência** · `43fb8a403d03`.

## Mapa do código

| Caminho | Papel de leitura | Símbolos selecionados |
| --- | --- | --- |
| [src/main/java/br/junior/agendamentos/AgendaProfissionais.java](../src/main/java/br/junior/agendamentos/AgendaProfissionais.java) | Implementação | `AgendaProfissionais`, `Estado`, `Reserva`, `Resumo`, `exigirDisponivel`, `validarTexto`, `comHorario`, `cancelar`, `ativa` |
| [src/main/java/br/junior/agendamentos/AgendaService.java](../src/main/java/br/junior/agendamentos/AgendaService.java) | Implementação | `AgendaService`, `disponivel`, `agendar`, `cancelar`, `listar`, `listarPorDia` |
| [src/main/java/br/junior/agendamentos/Agendamento.java](../src/main/java/br/junior/agendamentos/Agendamento.java) | Implementação | `Agendamento`, `conflita` |
| [src/main/java/br/junior/agendamentos/CalendarioTrabalho.java](../src/main/java/br/junior/agendamentos/CalendarioTrabalho.java) | Implementação | `CalendarioTrabalho`, `Expediente`, `validarDuracao`, `contem`, `minutos`, `definirExpediente`, `definirDiasUteis`, `expedientes`, `adicionarFeriado` |
| [src/main/java/br/junior/agendamentos/CatalogoServicos.java](../src/main/java/br/junior/agendamentos/CatalogoServicos.java) | Implementação | `CatalogoServicos`, `cadastrar`, `atualizar`, `obter`, `ativo`, `desativar`, `ativar`, `listar`, `listarAtivos` |
| [src/main/java/br/junior/agendamentos/Intervalo.java](../src/main/java/br/junior/agendamentos/Intervalo.java) | Implementação | `Intervalo`, `conflita`, `contem`, `intersecao`, `duracao`, `deslocar`, `compareTo` |
| [src/main/java/br/junior/agendamentos/RelatoriosAgenda.java](../src/main/java/br/junior/agendamentos/RelatoriosAgenda.java) | Implementação | `RelatoriosAgenda`, `Indicadores`, `porProfissional`, `demandaDiaria`, `csv`, `csvCampo`, `ics`, `formatar`, `escaparIcs` |
| [src/main/java/br/junior/agendamentos/Servico.java](../src/main/java/br/junior/agendamentos/Servico.java) | Implementação | `Servico`, `minutos`, `comPreco`, `comDuracao` |

## Configuração e referências

Ambiente e dependências: [pom.xml](../pom.xml).

Referências existentes: [docs/GUIA_DE_VALIDACAO.md](GUIA_DE_VALIDACAO.md).

## Roteiro de estudo e desenvolvimento

1. Comece pelo fluxo descrito acima e localize seus pontos de entrada no mapa. Acompanhe uma entrada válida até a transformação ou a resposta.
2. Abra um teste relacionado ao fluxo e descreva qual regra ele protege. Compare o tratamento esperado para entradas válidas, inválidas e nos limites.
3. Ao adicionar uma regra, determine qual módulo controla o dado e cubra o caso nesse nível. Em seguida, confira o efeito no comando, na API ou na interface de entrada.

**Pergunta de entrevista**

Quais casos de borda exercitam sobreposição, adjacência e cancelamento de horários?

## Testes versionados

Arquivos para começar a leitura dos cenários de teste: [src/test/java/br/junior/agendamentos/AgendaProfissionaisTest.java](../src/test/java/br/junior/agendamentos/AgendaProfissionaisTest.java), [src/test/java/br/junior/agendamentos/AgendaServiceConcorrenciaTest.java](../src/test/java/br/junior/agendamentos/AgendaServiceConcorrenciaTest.java), [src/test/java/br/junior/agendamentos/AgendaServiceTest.java](../src/test/java/br/junior/agendamentos/AgendaServiceTest.java), [src/test/java/br/junior/agendamentos/CatalogoCalendarioTest.java](../src/test/java/br/junior/agendamentos/CatalogoCalendarioTest.java), [src/test/java/br/junior/agendamentos/RelatoriosAgendaTest.java](../src/test/java/br/junior/agendamentos/RelatoriosAgendaTest.java).

Os comandos e versões de runtime usados no CI ficam em [.github/workflows/ci.yml](../.github/workflows/ci.yml).

[Voltar ao README](../README.md).
