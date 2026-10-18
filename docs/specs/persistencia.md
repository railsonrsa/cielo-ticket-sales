# Spec — Persistência

Origem: [Persistência e fonte dos Events](https://github.com/railsonrsa/cielo-ticket-sales/issues/6), com a revisão de Capacity feita em [Estratégia de testes](https://github.com/railsonrsa/cielo-ticket-sales/issues/7).

## Fonte dos Events

- `:core:data` traz `events.json` em assets, carregado no Room **uma única vez**, no `onCreate` do banco.
- Depois disso o Room é a única fonte. **O seed nunca é recarregado**: a Capacity é estado vivo, e recarregar desfaria vendas.
- As telas leem via repositório (interface em `:core:domain/repository`).

## Banco

Um Room só, sem DataStore.

| Tabela | Conteúdo |
|---|---|
| `event` | id, nome, data/hora, local, preço (centavos), Capacity |
| `cart_item` | Event + quantidade |
| `purchase` | id, estado, `FailureReason`, total, data/hora, `orderId`, `cieloCode`, `authCode`, `amount`, Pending Refund (+ `orderId`s extras), contagem de verificações inconclusivas, pendência do dialog de confirmação |
| `purchase_item` | Purchase, Event, quantidade, preço unitário (foto) |
| `ticket` | id, Purchase, Event |

- A aprovação atômica é um único `withTransaction`.
- **Capacity nunca abaixo de 0**: a aprovação atrasada sem lugar não emite nada e vira `SoldOutAfterPayment` + Pending Refund ([ADR 0001](../adr/0001-anti-duplicidade-e-reconciliacao.md), item 6).

## Migrações

`version = 1`, `exportSchema = true` (esquema versionado no repo). **Proibido usar `fallbackToDestructiveMigration`**: apagaria Purchases pendentes e marcas de Pending Refund.

## Process death

Tudo que importa está no Room. O callback da Cielo pode recriar o processo, então o fluxo de pagamento não depende de memória.

## Formatos

- `Money` = `Long` em centavos.
- Data/hora = epoch millis UTC, mostrada no fuso do aparelho.
- Ids = UUID em texto, gerados no app.
