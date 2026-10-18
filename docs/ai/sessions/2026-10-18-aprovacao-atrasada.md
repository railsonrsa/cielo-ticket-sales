# Aprovação atrasada e Pending Refund — 2026-10-18

**Prompt do usuário** (literal):
> vamos atuar no ticket #19

**Objetivo:** fatia [#19](https://github.com/railsonrsa/cielo-ticket-sales/issues/19): a aprovação que chega depois de a Purchase virar `Failed` é tratada sem nunca vender além da Capacity, e o dinheiro que saiu sem contrapartida fica visível.

## O que foi feito

- **Domínio em TDD** (14 testes vermelhos → verdes): os 3 desfechos do callback atrasado do [simulador](../../simuladores/aprovacao-atrasada.html) (com lugar → `Approved` sem mexer no Cart; sem lugar → `SoldOutAfterPayment` + Pending Refund; falta num Event só → a Purchase inteira vai para estorno), a `Abandoned` promovida, o valor atrasado divergente, a Order extra sobre uma Purchase já marcada para estorno e o callback repetido. O "Pagar" devolve `LateApprovalsToReview` até o Operator aceitar, e o aceite vale só para as Purchases que o dialog mostrou. Também o `AcknowledgeSoldOutUseCase` ("Entendi").
- **A aprovação no tempo certo também pode ficar sem lugar**, se uma atrasada levou os lugares enquanto ela esperava. Antes, isso derrubava o app (`check` no repositório). Agora o `approve` devolve `false` sem mudar nada, e o callback e a Reconciliation caem no mesmo `SoldOutAfterPayment` + Pending Refund.
- **`:core:data`:** esquema v5 (`needs_acknowledgment`) por `AutoMigration`, com teste do `MigrationTestHelper`. O teste Room confere que a Capacity nunca fica abaixo de 0 e que a aprovação atrasada mantém o Cart.
- **Telas:** no Cart, o aviso "Compra aprovada depois" e o dialog "Revisar carrinho" / "Continuar e pagar"; no Cart e em "Minhas compras", o dialog bloqueante de esgotado; em "Minhas compras", o selo vermelho com motivo, valor, códigos e `orderId`s a estornar. O texto do "Desistir" foi ajustado.
- **`:payment:fake`:** "Aprovar com atraso (30 s)", que fecha sem resposta e cobra depois.
- **Conferido no AVD** ([evidências](../../evidencias/19-aprovacao-atrasada/README.md)), o que achou um bug: o Cart navegava para "Pagamento aprovado" na aprovação atrasada da Purchase que ele mesmo tinha disparado. O bug foi corrigido com teste de ViewModel.

## Onde o humano corrigiu a IA

| Recomendação da IA | Decisão humana | Por quê |
|---|---|---|
| Ajustar o texto do "Desistir" (pendência do #18) em vez de mudar a regra | aceita | A regra do ADR 0001 fica; o texto passa a prometer ingressos se houver lugar e estorno se não houver |
| Dialog de esgotado global, acima do NavHost | **só no Cart e em "Minhas compras"** | Mais simples, e o aceite gravado garante que ele aparece até o "Entendi" |
| Fake com botão e timer de 30 s | aceita | Dá para demonstrar os dois desfechos sem notificação nem permissão nova |
| Promover qualquer `Failed` sem Pending Refund | aceita | A `Failed` com Pending Refund só acumula `orderId`s extras para estorno |

## Pendências

- Em "Minhas compras" aberta, uma Purchase que ganha Pending Refund sobe para o topo, e a `LazyColumn` mantém a âncora de rolagem: o selo dela fica acima da tela até o Operator rolar. Reabrir a tela resolve.
- A aprovação atrasada não foi vista na Cielo de verdade, só na fake: ela depende de a Cielo mandar o callback depois da Reconciliation.
