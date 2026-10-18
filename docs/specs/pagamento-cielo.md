# Spec — Pagamento com a Cielo LIO

Origem: [SDK da Cielo LIO](https://github.com/railsonrsa/cielo-ticket-sales/issues/2) ([pesquisa completa](../research/cielo-lio-sdk.md)), [Rodar o emulador](https://github.com/railsonrsa/cielo-ticket-sales/issues/9) ([roteiro e resultados](../emulador/roteiro-de-verificacao.md)) e [Estratégia contra cobrança duplicada](https://github.com/railsonrsa/cielo-ticket-sales/issues/4) ([ADR 0001](../adr/0001-anti-duplicidade-e-reconciliacao.md)).

## Integração: Deep Link, não SDK

O SDK Order Manager foi descontinuado e a política da Cielo exige `lio://`. **Nenhuma lib da Cielo entra no build.**

## Disparo

```
lio://payment?request=<base64(json)>&urlCallback=order://response
```

- JSON: `accessToken`, `clientID` (valores falsos funcionam no emulador), `reference` = `Purchase.id`, `value` em centavos = **soma exata dos Purchase Items** (a Cielo cobra o `value` e ignora os itens), `items[]` com `sku` = `Event.id`, `name`, `quantity`, `unitPrice`.
- Disparado **de dentro de um foreground service**, ligado do disparo até o callback.
- Antes de disparar, a Purchase `AwaitingPayment` já está gravada ([domínio](dominio.md)).

## Callback

Chega como `ACTION_VIEW` em `order://response?response=<base64>` na `LioCallbackActivity` sem UI ([ADR 0002](../adr/0002-porta-de-pagamento-so-de-disparo.md)), **possivelmente com o processo recém-criado**: tratar no `onCreate`, só com o que está em disco.

Decidir pela **forma do JSON**, nunca pelo `responsecode` (vem `0` no sucesso e no cancelamento, e ausente no erro):

| JSON | Tradução (`PaymentOutcome`) |
|---|---|
| `id` + `payments[]` | Cielo Order. **Autorizado** se a soma de `payments[].amount` com `paymentFields.statusCode` ∈ {`"0"`,`"1"`} (string) == total. Ignorar `status` e `paidAmount` (vem `0` mesmo `PAID`). Soma ≠ total → `AmountMismatch` + Pending Refund |
| `code` + `reason` | falha: `1` → `CanceledByOperator`, `2` → `PaymentError`, `3` → `Declined`, `4` → `TerminalAuthError`. **Não traz `reference`**: casar com a única `AwaitingPayment` |
| ausente / Base64 ou JSON inválido | ilegível → a Purchase continua `AwaitingPayment` (log) |

`ActivityNotFoundException` no disparo → `TerminalUnavailable`. Antes disso, uma checagem prévia via `<queries>` evita criar a Purchase.

## Reconciliation (`lio://orders`)

- **Quando**: na abertura a frio do app com uma `AwaitingPayment` (a primeira `MainActivity` do processo), e pelo botão "Verificar pagamento". **Nunca no resume.**
- **Disparo**: `lio://orders?request=<base64({accessToken, clientID, pageSize: 5, page})>&urlCallback=order://orders`, sem o foreground service. A resposta volta num **host próprio** (`order://orders`), na mesma `LioCallbackActivity`: um `{code, reason}` da consulta vira verificação inconclusiva, nunca falha de pagamento (decidido no [#18](https://github.com/railsonrsa/cielo-ticket-sales/issues/18)).
- **Busca em memória**: o `ReconcilePurchaseUseCase` é único no app e pede a próxima página a cada resposta. Sem resposta em 15 s, a verificação é inconclusiva. Se o processo morrer no meio, a busca se perde sem contar, e a próxima abertura a frio recomeça.
- `lio://orders` é silencioso (~40 ms), paginado de 5 em 5, com `totalItems`/`totalPages` e o `reference` em cada Order. Pagamentos cancelados ou abandonados também viram Order, com `status: "ENTERED"`.
- **Busca das duas pontas para o meio** (páginas 1, N, 2, N-1…), porque a ordenação não é documentada. Para quando acha o `reference` (lendo também a página vizinha, para pegar duplicatas), quando cada ponta passa do `createdAt` da Purchase (com folga de alguns minutos), quando as pontas se encontram, ou ao chegar a 20 páginas (inconclusivo).
- **Desfechos**: soma autorizada == total → `Approved`; só `ENTERED` → `NotCompleted`; nenhuma Order → `NotStarted`; consulta falhou ou foi inconclusiva → continua `AwaitingPayment`; duas Orders pagas → `Approved` com a primeira + Pending Refund; valor divergente → `AmountMismatch` + Pending Refund.
- **Atalho de parada**: o trecho não lido fica entre o último `createdAt` lido de cada ponta; se ele não chega a 10 min da hora da Purchase, a busca para. Vale para as duas ordens, e só quando as duas pontas trazem `createdAt`.
- **C12b** (rodado no #18): a ordem **se mantém** entre as páginas (mais antiga primeiro); toda Order traz `createdAt` no formato `Oct 18, 2026 4:52:48 PM`, na hora local do aparelho; a página 1 vem com **6** Orders (a semente `id: "1"` a mais), e **a última da página 1 se repete como a primeira da página 2**. A busca conta cada `orderId` uma vez só.

## Manifest (todo em `:payment:cielo-lio`)

- intent-filters `order://response` e `order://orders` na `LioCallbackActivity`
- `<queries>` com `com.ads.lio.uriappclient` **e** `br.com.cielosmart.orderservice` (é este que atende no emulador)
- meta-data `cs_integration_type=uri`
- o foreground service do disparo

## Plataforma

`minSdk 24`, `targetSdk 29` (recomendação da Cielo; dispensa o tipo do FGS e a permissão de notificação), `compileSdk 35`. Sem Google Play Services e sem WebView. O emulador roda num AVD Android 10 ARM64 sem GMS.
