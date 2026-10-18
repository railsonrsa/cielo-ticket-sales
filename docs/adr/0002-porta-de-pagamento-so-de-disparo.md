# 0002 — Porta de pagamento só de disparo, callback numa Activity do módulo Cielo

**Status:** aceito (17/10/2026) · Ticket: [Arquitetura e modularização](https://github.com/railsonrsa/cielo-ticket-sales/issues/5)

## Contexto

A integração com a Cielo é por Deep Link (`lio://payment`, `lio://orders`). A resposta **não volta como retorno de função**: chega depois como uma intent nova (`order://response`), às vezes num processo que o Android acabou de recriar (cenário C9 do emulador). Se a porta fosse `suspend fun pay(): PaymentResult`, ela prometeria algo que a plataforma não garante: a coroutine que esperava o resultado pode não existir mais quando o callback chega.

## Decisão

1. **A porta só dispara.** `PaymentTerminal`, em `:core:domain/terminal`, expõe `requestPayment(purchase)` e `requestOrders(page)` e não devolve o desfecho.
2. **A resposta entra por outro caminho.** O callback é traduzido para um tipo do domínio (`PaymentOutcome`, `OrdersPage`) e entregue a um use case (`HandlePaymentOutcomeUseCase`, `ReconcilePurchaseUseCase`), que lê e grava a Purchase no disco. A tela observa o banco, não o callback.
3. **O callback entra por uma `LioCallbackActivity` sem UI**, dentro de `:payment:cielo-lio`. Ela decodifica o Base64, chama o use case, abre a `MainActivity` e se fecha.
4. **O módulo Cielo carrega tudo que é da Cielo no próprio manifest**: intent-filter `order://response`, `<queries>`, `cs_integration_type` e o foreground service. O `:app` não sabe que a Cielo existe.
5. Uma `FakePaymentTerminal` (`:payment:fake`) implementa a mesma porta e entrega a resposta pelos mesmos use cases, para testes e para demonstrar o app sem o emulador.

## Alternativas descartadas

- `suspend fun pay()`: o resultado se perde quando o processo é recriado entre o disparo e o callback.
- Callback na `MainActivity` (`singleTop` + `onNewIntent`): mistura o parsing da Cielo com a navegação da UI e espalha o conhecimento da Cielo pelo `:app`.

## Consequências

- O estado do pagamento vive só no disco, o que conversa com o "gravar antes de disparar" do [ADR 0001](0001-anti-duplicidade-e-reconciliacao.md).
- Trocar o meio de pagamento (outro terminal, outro adquirente) se resume a outro módulo que implementa `PaymentTerminal` e entrega a resposta aos mesmos use cases.
- A UI nunca "espera" o pagamento: ela mostra a Purchase `AwaitingPayment` até o banco mudar.
