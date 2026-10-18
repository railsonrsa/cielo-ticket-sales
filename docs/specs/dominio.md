# Spec — Domínio

Estado atual do modelo. Origem: [Modelo de domínio e ciclo de vida da Purchase](https://github.com/railsonrsa/cielo-ticket-sales/issues/3), com as revisões de [Estratégia contra cobrança duplicada](https://github.com/railsonrsa/cielo-ticket-sales/issues/4), [Estratégia de testes](https://github.com/railsonrsa/cielo-ticket-sales/issues/7) e [Mensagens de falha](https://github.com/railsonrsa/cielo-ticket-sales/issues/10). Os termos estão no [`GLOSSARY.md`](../../GLOSSARY.md).

## Entidades

| Entidade | Campos | Observações |
|---|---|---|
| **Event** | `id`, nome, data/hora, local, preço unitário (`Money`), Capacity | vem do seed (ver [persistência](persistencia.md)) |
| **Cart** | itens: Event + quantidade, **sem preço** | persistido em disco; não é Purchase e não reserva nada |
| **Purchase** | `id` (UUID gerado no "Pagar"), estado, itens, total, data/hora, dados da Cielo Order, marca Pending Refund | `id` vai como `reference` no deep link |
| **Purchase Item** | Event, quantidade, **preço unitário do momento do "Pagar"** | mudar o preço do Event depois não altera compras antigas |
| **Ticket** | `id` (UUID), Purchase, Event | **1 por unidade**; o QR é só o UUID, sem assinatura |
| **Money** | value class, centavos em `Long`, só BRL | |

## Máquina de estados da Purchase

```
Cart ──"Pagar"──▶ AwaitingPayment ──callback autorizado──▶ Approved
                        │
                        ├─ callback de falha / Reconciliation conclusiva ──▶ Failed(FailureReason)
                        ├─ "Desistir desta compra" (após ≥1 verificação inconclusiva) ──▶ Failed(Abandoned)
                        └─ sem callback / callback ilegível / inconclusivo ──▶ continua AwaitingPayment

Failed ──callback autorizado atrasado + Capacity para todos os itens──▶ Approved
Failed ──callback autorizado atrasado + falta Capacity──▶ Failed(SoldOutAfterPayment) + Pending Refund
```

- **Três estados**: `AwaitingPayment`, `Approved`, `Failed`. O motivo é um dado (`FailureReason`), não um estado. Não existe `Unverified`.
- **Invariante: uma `AwaitingPayment` por vez.** Enquanto ela existir, o "Pagar" fica bloqueado.
- **Transições só a partir de `AwaitingPayment`**; um `Approved` ignora callbacks repetidos. A única exceção é a promoção atrasada `Failed` → `Approved` ([ADR 0001](../adr/0001-anti-duplicidade-e-reconciliacao.md), item 6).
- **Retry após `Failed`** = Purchase nova, com UUID novo, a partir do mesmo Cart.

### `FailureReason` (tipo fechado)

`CanceledByOperator`, `PaymentError`, `Declined`, `TerminalAuthError`, `TerminalUnavailable`, `NotCompleted`, `NotStarted`, `AmountMismatch`, `SoldOutAfterPayment`, `Abandoned`. O texto para o Operator está em [falhas](falhas.md).

### Pending Refund

Uma marca separada do estado. Vale para `Failed(AmountMismatch)`, `Failed(SoldOutAfterPayment)` e para `Approved` com cobrança duplicada (`orderId`s extras ficam gravados). O estorno em si fica fora do escopo.

## Regras

- **Cart**: só esvazia na aprovação normal. Se a Purchase falhar, o Cart fica intacto. A tela limita a quantidade à Capacity atual; um item esgotado bloqueia o "Pagar" até ser ajustado ou removido.
- **Capacity no "Pagar"**: o use case revalida, sem reservar, e devolve os Events em conflito. Nesse caso nenhuma Purchase é criada.
- **Aprovação atômica**: uma única transação com estado → `Approved`, Tickets (1 por unidade), decremento da Capacity de cada Event, Cart esvaziado e dados da Cielo Order gravados (`orderId`, `cieloCode`, `authCode`, `amount`). Ou acontece tudo, ou nada.
- **Aprovação atrasada** (`Failed` → `Approved`): emite Tickets e decrementa a Capacity a partir dos Purchase Items, **não mexe no Cart** e grava a pendência de um dialog de confirmação ("Revisar carrinho" / "Continuar e pagar") que aparece no próximo "Pagar" até o Operator aceitar.
- **Capacity nunca fica abaixo de 0**, e nunca há mais Tickets que lugares.

## O que o Operator vê

- **Comprovante** (só `Approved`): itens (Event × qtd × preço unitário), total, data/hora, `authCode`/`cieloCode` e os Tickets com QR.
- **"Minhas compras"**, nesta ordem: (1) a `AwaitingPayment` no topo, com "Verificar pagamento"; (2) as Purchases com Pending Refund, em destaque; (3) as `Approved`, da mais recente para a mais antiga. As `Failed` sem dinheiro envolvido ficam no banco, mas não aparecem.
