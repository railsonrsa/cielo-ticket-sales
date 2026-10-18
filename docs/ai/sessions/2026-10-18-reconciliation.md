# Reconciliation e saída Abandoned — 2026-10-18

**Prompt do usuário** (literal):
> Vamos atuar no próximo ticket #18

**Objetivo:** fatia [#18](https://github.com/railsonrsa/cielo-ticket-sales/issues/18): a Purchase órfã descobre o próprio desfecho consultando a Cielo, e o Operator tem uma saída quando a Cielo não responde.

## O que foi feito

- **Domínio em TDD** (19 testes vermelhos → verdes): `ReconcilePurchaseUseCase` com os seis desfechos, a busca das duas pontas para o meio (`OrdersSearch`, função pura), as vizinhas da página que contém o `reference`, o atalho de parada pelo `createdAt` nas duas ordens, o limite de 20 páginas, o tempo limite de 15 s, a abertura a frio uma vez por processo e o callback que chega no meio da busca (ele vence). Também o `AbandonPurchaseUseCase`, bloqueado antes da 1ª verificação inconclusiva e durante uma. A porta ganhou `requestOrders(page)` e o tipo `OrdersPage`.
- **`:core:data`:** esquema v4 (`inconclusive_checks`, `extra_order_ids`) por `AutoMigration`, com teste do `MigrationTestHelper`. A aprovação grava os `orderId`s pagos a mais com a marca Pending Refund.
- **`:payment:cielo-lio`:** `lio://orders` com resposta em `order://orders`, `OrdersResponseTranslator` (núcleo de JSON comum com o do pagamento) e **fixtures capturadas no emulador** pelo próprio callback do app, porque o log da Cielo corta em 4 KB.
- **`:payment:fake`:** a "Cielo" da demo guarda as Orders em memória (`ENTERED` no disparo, paga na aprovação) e responde ao `requestOrders`.
- **Telas:** "Verificar pagamento" no banner do Cart e no topo de "Minhas compras"; depois de uma verificação inconclusiva, o aviso e o "Desistir desta compra" com confirmação. Gatilho da abertura a frio na `MainActivity`, com um teste Robolectric de que o resume e a rotação não disparam nada.
- **Conferido no emulador** ([evidências](../../evidencias/18-reconciliation/README.md)): órfã real (Cielo cobrou, callback desligado), verificação inconclusiva, zero consultas no resume, aprovação na abertura a frio e `NotCompleted` depois do Voltar. O **C12b** rodou: ordem mantida entre as páginas, `createdAt` em hora local, e a página 1 repete a última Order na página 2.

## Onde o humano corrigiu a IA

| Recomendação da IA | Decisão humana | Por quê |
|---|---|---|
| Resposta do `lio://orders` em `order://orders`, não no `order://response` | aceita | Um `{code, reason}` da consulta seria lido como falha de pagamento |
| A fake responde à Reconciliation com um log de Orders em memória | aceita | O inconclusivo e o `Abandoned` ficam nos testes e no emulador; uma tela de escolha custaria mais |
| Capturar as fixtures e rodar o C12b no emulador já nesta sessão | aceita | Fixtures capturadas valem mais que as derivadas |

## Pendências

- **Texto da confirmação do "Desistir":** a spec de falhas promete que, "se a cobrança tiver passado, ela vai ficar marcada para estorno". Mas, depois do `Abandoned`, nada reconsulta a Cielo, e o item 6 do ADR 0001 aprovaria uma aprovação atrasada com lugar, sem estorno. Precisa de decisão humana: ajustar o texto ou reconsultar depois.
- O C12b rodou com 10 Orders (2 páginas). Uma lista com mais de 3 páginas no emulador ainda não foi vista.
- A promoção atrasada `Failed` → `Approved` e os selos de Pending Refund ficam no #19.
