# Evidências — Integração Cielo LIO (#17)

Capturadas em 18/10/2026 no AVD `Cielo_API29` (Android 10, ARM64, sem GMS), com o emulador Cielo 1.61.8 e o flavor `cieloDebug` da branch `impl/17-cielo-lio`, instalado por cima do banco v3. Os cenários seguem o [roteiro do emulador](../../emulador/roteiro-de-verificacao.md); o C2, o C3, o C6, o C8 e o "app ausente" foram rodados pelo humano, e o C1 e o C7 pela IA, via `adb`.

| Print | Cenário | Conferido no banco (`sqlite3`) e no `logcat` |
|---|---|---|
| 01 | "Pagar" → a Cielo recebe a soma exata (R$ 40,00) | `lio://payment` disparado de dentro do FGS (ícone ⓘ na barra) |
| 02 | Escolha do desfecho no emulador | o emulador só oferece Sucesso, Cancelado e Erro: os códigos 3 e 4 não são geráveis |
| 03 | C1 · Sucesso → de volta ao app, na tela de resultado | `APPROVED` com `order_id`/`auth_code`/`cielo_code`, 1 Ticket, Capacity 146→145, Cart vazio, FGS desligado, uma `MainActivity` só |
| — | C2 · Cancelado | `FAILED / CanceledByOperator`, Cart intacto |
| — | C3 · Erro → Tentar de novo → Erro → Tentar de novo → Sucesso | 2 × `FAILED / PaymentError` e um `APPROVED`, cada um com UUID novo |
| — | C6 · 2 Events, R$ 140,00 | `APPROVED`, autorizado 14000 = total, 3 Tickets |
| — | C8 · `am kill` com a tela da Cielo aberta → Cancelado | o processo sobreviveu (mesmo pid); o callback chegou e virou `FAILED / CanceledByOperator` |
| — | App da Cielo desativado (`pm disable-user`) → Pagar | dialog "App de pagamento da Cielo não encontrado"; nenhuma Purchase criada |
| 04 | C7 · Voltar ◀ na tela da Cielo | nenhum callback em 30 s+; `AWAITING_PAYMENT`, banner e "Pagar" bloqueado. A órfã foi marcada `Abandoned` via `sqlite3` para destravar o AVD (a saída de verdade é o #18) |
