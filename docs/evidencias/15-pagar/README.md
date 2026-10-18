# Evidências — Pagar com a FakePaymentTerminal (#15)

Capturadas em 18/10/2026 no AVD Android 10 (API 29), com o build da branch `impl/15-pagar`, instalado por cima da v2 do banco.

| Print | Cenário | Conferido no banco (`sqlite3`) |
|---|---|---|
| 01–02 | Catálogo e Cart (2 × Choro + 1 × Marés = R$ 140,00) | — |
| 03 | "Pagar" abre a maquininha simulada | Purchase `AWAITING_PAYMENT` de 14000 já gravada **antes** da escolha |
| 04 | Aprovar → tela de resultado | `APPROVED` com o `auth_code`; `cart_item` vazio |
| 05 | Cancelar (1) → snackbar, Cart intacto | `FAILED / CanceledByOperator` |
| 06 | Erro genérico (2) → dialog com retry | `FAILED / PaymentError` |
| 07 | Negado (3) → dialog com retry | `FAILED / Declined` |
| 08 | Tentar de novo → maquininha com outra referência | Purchase nova `AWAITING_PAYMENT`, UUID diferente |
| 09 | Sem autenticação (4) → dialog sem retry | `FAILED / TerminalAuthError` |
| 10 | Valor divergente → dialog de estorno | `FAILED / AmountMismatch`, `pending_refund = 1`, autorizado 2000 de 4000 |
| 11 | Resposta ilegível → banner, "Pagar" bloqueado | continua `AWAITING_PAYMENT` |
| 12 | Voltar na maquininha → banner, "Pagar" bloqueado | continua `AWAITING_PAYMENT` |

Entre o 11 e o 12, a órfã foi marcada `FAILED / Abandoned` direto no banco (e o app reaberto), porque o "Verificar pagamento" só entra no #18.
