# Planejamento (mapa de decisões) — 16 e 17/10/2026

> **Diário retroativo.** As sessões de planejamento aconteceram antes de existir o formato de diário. Ele foi reconstruído pela IA a partir dos tickets fechados e precisa ser **revisado pelo humano**. Os prompts das sessões antigas não foram preservados literalmente; só os da última sessão (abaixo) são citações exatas.

**Prompts do usuário na sessão que fechou o mapa** (literais):
> Estrutura do harness de IA no repositório vamos para esse próximo ticket

> sim para todos

> podemos seguir assim

**Objetivo:** sair de uma ideia solta ("app de ingressos com Cielo LIO") para uma spec pronta para implementar, em menos de dois dias.

## O que foi feito

- **Mapa** charted em [issue #1](https://github.com/railsonrsa/cielo-ticket-sales/issues/1), com 9 tickets: 1 de pesquisa, 1 tarefa (emulador) e 7 de grilling. Todos fechados; o fog que sobrou (design visual, analytics, monitoring, criptografia) foi tirado de escopo por causa do prazo.
- **Pesquisa da Cielo** num subagente ([`docs/research/cielo-lio-sdk.md`](../../research/cielo-lio-sdk.md)): Deep Link em vez do SDK descontinuado.
- **Emulador** em sessão assistida ([roteiro](../../emulador/roteiro-de-verificacao.md), sonda em [`spikes/lio-probe`](../../../spikes/lio-probe/)): a Cielo cobra duas vezes o mesmo `reference`; o `paidAmount` vem 0 mesmo em pagamento aprovado; Voltar não devolve callback.
- **Decisões duráveis:** [ADR 0001](../../adr/0001-anti-duplicidade-e-reconciliacao.md) (anti-duplicidade e Reconciliation), [ADR 0002](../../adr/0002-porta-de-pagamento-so-de-disparo.md) (porta de pagamento só de disparo), [`GLOSSARY.md`](../../../GLOSSARY.md) com 12 termos.
- **Simulador** da aprovação atrasada ([`docs/simuladores/aprovacao-atrasada.html`](../../simuladores/aprovacao-atrasada.html)) para decidir a regra de Capacity.
- **Harness:** `AGENTS.md`, `docs/specs/` por área, esta pasta `docs/ai/` e o README como hub.

## Onde o humano corrigiu a IA

| Recomendação da IA | Decisão humana | Por quê |
|---|---|---|
| Ler `lio://orders` assumindo "mais antiga primeiro" (como no emulador) | Buscar pelas duas pontas para o meio | O humano questionou: só vimos a página 1 e a ordenação não é documentada ([ADR 0001](../../adr/0001-anti-duplicidade-e-reconciliacao.md), item 4) |
| Aprovação atrasada sempre promove `Failed`→`Approved` (Capacity negativa, depois `max(0, …)`) | Só promove se houver lugar para todos os itens; senão estorno, tudo ou nada | O humano levantou "quem confirma primeiro leva": não emitir Ticket além da lotação ([Estratégia de testes](https://github.com/railsonrsa/cielo-ticket-sales/issues/7)) |
| Aprovação atrasada só com alerta | + dialog de confirmação no próximo "Pagar", gravado em disco | Pedido do humano: o Cart não esvazia nesse caso e o Operator poderia cobrar de novo sem perceber |

## Pendências

- Cenário **C12b** no emulador (ordem nas páginas 2 e última de `lio://orders`, formato do `createdAt`).
- Implementação: começar pelas fatias de `:core:domain` com TDD.
