# Spec — Testes

Origem: [Estratégia de testes dos cenários críticos](https://github.com/railsonrsa/cielo-ticket-sales/issues/7).

## Pirâmide

| Nível | Onde | O que prova |
|---|---|---|
| **Base: use cases** (JUnit puro) | `:core:domain` | Pagar + trava de uma `AwaitingPayment` por vez (duplo toque, retry); callback idempotente; callback atrasado nos 3 desfechos (com lugar → `Approved`; sem lugar → `SoldOutAfterPayment` + Pending Refund; falta lugar num só Event → a Purchase inteira vai para estorno); desfechos da Reconciliation (aprovada, `ENTERED`, nenhuma Order, inconclusiva, duplicata, valor divergente); busca pelas duas pontas; saída `Abandoned` |
| **Room real** (Robolectric, em memória) | `:core:data` | aprovação atômica (Tickets 1 por unidade + Capacity + Cart) contra SQL de verdade; seed carregado uma vez |
| **ViewModels** (`kotlinx-coroutines-test` + Turbine) | `:feature:*` | `UiState` e efeitos |
| **Tradução da Cielo** | `:payment:cielo-lio` | Base64 → `PaymentOutcome` (códigos 1–4, ilegível) e `lio://orders` → `OrdersPage`, com **fixtures capturadas no emulador** (inclui `paidAmount = 0` com `PAID` e cobrança duplicada) |
| **Processo recriado** (Robolectric) | `:payment:cielo-lio` | 1 teste: abrir a `LioCallbackActivity` com intent de fixture e conferir o banco |
| **Topo: UI Compose** | `:app` | 1 teste do caminho feliz: Cart → Pagar → aprovada → Minhas compras, com a `FakePaymentTerminal` |
| **Grafo de DI** | `:app` | `verify()` do Koin |

O emulador da Cielo fica fora da automação: é manual, em [`docs/emulador/roteiro-de-verificacao.md`](../emulador/roteiro-de-verificacao.md).

## Dublês

**Só fakes escritos à mão** (repositórios em memória + `FakePaymentTerminal`), sem MockK. Os cenários críticos dependem de estado, e um fake confere o resultado, não a chamada.

## Bibliotecas

JUnit4 (um runner só, por causa do Robolectric e do Compose test), Truth, Turbine, `kotlinx-coroutines-test`, Robolectric, Compose `ui-test`, Koin test.

## CI e ordem

- GitHub Actions rodando `./gradlew test lint` a cada push, sem meta de cobertura.
- TDD em `:core:domain`; o resto ganha testes logo depois de cada fatia.
