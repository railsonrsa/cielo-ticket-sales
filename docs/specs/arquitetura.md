# Spec — Arquitetura e modularização

Origem: [Arquitetura e modularização](https://github.com/railsonrsa/cielo-ticket-sales/issues/5) e [ADR 0002](../adr/0002-porta-de-pagamento-so-de-disparo.md).

## Módulos

| Módulo | Papel | Packages |
|---|---|---|
| `:app` | `MainActivity`, `NavHost`, montagem do Koin | |
| `:core:domain` | Kotlin puro: modelo, regras, use cases, portas | `model/`, `repository/` (interfaces), `terminal/` (`PaymentTerminal`), `usecase/{catalog,cart,payment,purchase}` |
| `:core:data` | Room, seed, implementações dos repositórios, transação de aprovação | `repository/`, `local/{database,dao,entity}`, `seed/`, `mapper/`, `di/` |
| `:core:ui` | tema Material 3, componentes compartilhados | |
| `:payment:cielo-lio` | implementação real da porta + callback | `CieloLioTerminal`, `deeplink/`, `callback/`, `service/`, `di/` |
| `:payment:fake` | `FakePaymentTerminal` para testes e demo sem emulador | |
| `:feature:catalog` | lista de Events | `presentation/{navigation,<tela>,components}`, `di/` |
| `:feature:checkout` | Cart, Pagar, resultado | idem |
| `:feature:purchases` | Minhas compras, comprovante, Ticket/QR | idem |

**Por que a camada fica por módulo, e não por feature:** a aprovação atômica (Tickets + Capacity + Cart) atravessa as três features e é disparada pelo módulo Cielo, então o data precisa de um dono só. Pode ser revisto durante a implementação.

## Regras de dependência

- Uma feature só enxerga `:core:domain` e `:core:ui`. Uma feature não conhece outra: a ligação é por callbacks de navegação, montados no `:app`.
- Tudo é `internal`, menos a navegação e o módulo Koin de cada módulo.
- `:core:domain` não depende de Android.
- O `:app` não sabe que a Cielo existe: todo o manifest da Cielo mora em `:payment:cielo-lio`.

## Porta de pagamento ([ADR 0002](../adr/0002-porta-de-pagamento-so-de-disparo.md))

- `PaymentTerminal.requestPayment(purchase)` e `requestOrders(page)` só disparam.
- `PaymentTerminal.isAvailable()` é a checagem prévia do "Pagar": sem o app da Cielo, o `StartPurchaseUseCase` devolve `TerminalUnavailable` sem criar a Purchase. Não é desfecho de pagamento; a corrida com o `ActivityNotFoundException` vira `PaymentOutcome.Failed(TerminalUnavailable)` pelo use case.
- O callback vira `PaymentOutcome` / `OrdersPage` e entra em `HandlePaymentOutcomeUseCase` / `ReconcilePurchaseUseCase`.
- A tela observa o banco, nunca o callback.

## UI

- Compose + MVVM. Cada tela tem `XScreen` (com estado, `koinViewModel`), `XContent` (sem estado, com preview), `XViewModel`, `XUiState` (único, em `StateFlow`), `XAction` e `XEffect` (`Channel`).
- Uma Activity só; Navigation Compose com rotas `@Serializable`.
- O ViewModel não guarda nada que não possa reler do Room. `SavedStateHandle` só para estado de tela descartável.

## DI

Koin (não depende de GMS e nenhum requisito exige Hilt), com um teste `verify()` do grafo.

## Build

- Version catalog + convention plugins em `build-logic` (`android-library`, `android-feature`, `jvm-library`).
- `minSdk 24`, `compileSdk 35`, `targetSdk 29`. JDK 17.
- **Flavors `demo`/`cielo`** (dimensão `terminal`, decidido no [#17](https://github.com/railsonrsa/cielo-ticket-sales/issues/17)): o `demo` depende só de `:payment:fake` e ganha o sufixo `.demo`; o `cielo` depende só de `:payment:cielo-lio`. Cada flavor tem o seu `paymentModule` Koin em `app/src/<flavor>`, e o `:app` só o inclui. O APK `demo` não leva o manifest da Cielo, nem o `cielo` leva a fake.

## Bibliotecas

Compose (BOM), Navigation Compose, Koin, Room, kotlinx-serialization, kotlinx-coroutines. Testes em [testes](testes.md).
**A escolher na implementação:** gerador de QR sem GMS (ex.: ZXing `core`).
