# Evidências — Reconciliation e saída `Abandoned` (#18)

Capturadas em 18/10/2026 no AVD `Cielo_API29` (Android 10, sem GMS), com o emulador Cielo 1.61.8 e o APK `cielo` da branch `impl/18-reconciliation`, sobre o banco da sessão anterior (migrado de v3 para v4 sem perder nada: `PRAGMA user_version` = 4).

| Print | Cenário | Conferido |
|---|---|---|
| 01 | Órfã de verdade: `LioCallbackActivity` desligada (`pm disable`), pagamento de R$ 40 com "Sucesso" na Cielo. A Cielo chamou o callback e ninguém atendeu | logcat da Cielo: `calling integrator url callback`; `sqlite3`: `AWAITING_PAYMENT` |
| 02 | "Verificar pagamento" com o callback ainda desligado: a consulta sai, e a resposta não volta | botão "Verificando…" desabilitado |
| 03 | 15 s depois: verificação inconclusiva, com o aviso e o "Desistir desta compra" | `inconclusive_checks = 1`, continua `AWAITING_PAYMENT` |
| — | **Resume:** Home e volta ao app | `GetOrdersActivity` no logcat: **0** |
| — | **Abertura a frio:** callback religado, `force-stop`, abrir o app | 3 consultas `lio://orders` (páginas 1, 3 e a vizinha 2); `APPROVED` com o `orderId` pago, 1 Ticket, Capacity 141 → 140, Cart vazio; o app fica no catálogo |
| 04 | Órfã do Voltar na tela da Cielo (C7): "Verificar pagamento" no topo de "Minhas compras" | — |
| 05 | Depois da verificação: `NotCompleted` (a Cielo guardou a Order como `ENTERED`), a órfã some da lista | `FAILED / NotCompleted / pending_refund = 0`; o Cart continua com o item |

O "Desistir desta compra" em si (confirmação → `Failed(Abandoned)`) ficou coberto pelos testes de use case e de ViewModel; no emulador, só a aparição do botão.
